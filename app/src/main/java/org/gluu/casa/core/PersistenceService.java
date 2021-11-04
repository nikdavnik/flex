package org.gluu.casa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.gluu.casa.model.ApplicationConfiguration;
import org.gluu.casa.core.model.CustomScript;
import org.gluu.casa.core.model.GluuOrganization;
import org.gluu.casa.core.model.oxAuthConfiguration;
import org.gluu.casa.core.model.Person;
import org.gluu.casa.misc.Utils;
import org.gluu.casa.service.IPersistenceService;
import org.oxauth.persistence.model.configuration.GluuConfiguration;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.PersistenceEntryManagerFactory;
import org.gluu.persist.ldap.operation.LdapOperationService;
import org.gluu.persist.model.PersistenceConfiguration;
import org.gluu.persist.model.SearchScope;
import org.gluu.persist.model.base.SimpleBranch;
import org.gluu.persist.service.PersistanceFactoryService;
import org.gluu.service.cache.CacheConfiguration;
import org.gluu.service.document.store.conf.DocumentStoreConfiguration;
import org.gluu.util.properties.FileConfiguration;
import org.gluu.util.security.PropertiesDecrypter;
import org.gluu.util.security.StringEncrypter;
import org.gluu.search.filter.Filter;
import org.jboss.weld.inject.WeldInstance;
import org.slf4j.Logger;

@ApplicationScoped
public class PersistenceService implements IPersistenceService {

    private static final int RETRIES = 15;
    private static final int RETRY_INTERVAL = 15;
    private static final String DEFAULT_CONF_BASE = "/etc/gluu/conf";

    @Inject
    private Logger logger;

    @Inject
    private PersistanceFactoryService persistanceFactoryService;

    @Inject
    private WeldInstance<PersistenceEntryManagerFactory> pFactoryInstance;

    private PersistenceEntryManager entryManager;

    private LdapOperationService ldapOperationService;

    private String rootDn;

    private JsonNode oxAuthConfDynamic;

    private JsonNode oxAuthConfStatic;

    private Set<String> personCustomObjectClasses;

    private ObjectMapper mapper;

    private StringEncrypter stringEncrypter;

    private CacheConfiguration cacheConfiguration;
    private DocumentStoreConfiguration documentStoreConfiguration;

    public boolean initialize() {

        boolean success = false;
        try {
            mapper = new ObjectMapper();
            success = setup(RETRIES, RETRY_INTERVAL);
            logger.info("PersistenceService was{} initialized successfully", success ? "" : " not");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    public <T> List<T> find(Class<T> clazz, String baseDn, Filter filter, int start, int count) {

        try {
            return entryManager.findEntries(baseDn, clazz, filter, SearchScope.SUB, null, null, start, count, 0);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public <T> List<T> find(Class<T> clazz, String baseDn, Filter filter) {

        try {
            return entryManager.findEntries(baseDn, clazz, filter);
        } catch (Exception e) {
            //logger.error(e.getMessage(), e);
            //TODO: uncomment the above once https://github.com/GluuFederation/oxCore/issues/160 is solved
            logger.error(e.getMessage());
            return Collections.emptyList();
        }

    }

    public <T> List<T> find(T object) {

        try {
            return entryManager.findEntries(object);
        } catch (Exception e) {
            //logger.error(e.getMessage(), e);
            //TODO: uncomment the above once https://github.com/GluuFederation/oxCore/issues/160 is solved
            logger.error(e.getMessage());
            return Collections.emptyList();
        }
    }

    public <T> int count(T object) {

        try {
            return entryManager.countEntries(object);
        } catch (Exception e) {
            //logger.error(e.getMessage(), e);
            //TODO: uncomment the above once https://github.com/GluuFederation/oxCore/issues/160 is solved
            logger.warn(e.getMessage());
            return -1;
        }

    }

    public <T> boolean add(T object) {

        try {
            entryManager.persist(object);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }

    public <T> T get(Class<T> clazz, String dn) {

        try {
            return entryManager.find(clazz, dn);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

    }

    public <T> boolean modify(T object) {

        try {
            entryManager.merge(object);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }

    public <T> boolean delete(T object) {

        try {
            entryManager.remove(object);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

    }

    public PersistenceEntryManager getEntryManager() {
        return entryManager;
    }

    public Map<String, String> getCustScriptConfigProperties(String acr) {
        return Optional.ofNullable(getScript(acr)).map(Utils::scriptConfigPropertiesAsMap).orElse(null);
    }

    public String getPersonDn(String id) {
        return String.format("inum=%s,%s", id, getPeopleDn());
    }

    public String getPeopleDn() {
        return jsonProperty(oxAuthConfStatic, "baseDn", "people");
    }

    public String getGroupsDn() {
        return jsonProperty(oxAuthConfStatic, "baseDn", "groups");
    }

    public String getClientsDn() {
        return jsonProperty(oxAuthConfStatic, "baseDn", "clients");
    }

    public String getScopesDn() {
        return jsonProperty(oxAuthConfStatic, "baseDn", "scopes");
    }

    public String getCustomScriptsDn() {
        return jsonProperty(oxAuthConfStatic, "baseDn", "scripts");
    }

    public GluuOrganization getOrganization() {
        return get(GluuOrganization.class, rootDn);
    }

    public String getIssuerUrl() {
        return jsonProperty(oxAuthConfDynamic, "issuer");
    }

    public Set<String> getPersonOCs() {
        return personCustomObjectClasses;
    }

    public boolean isAdmin(String userId) {
        GluuOrganization organization = getOrganization();
        List<String> dns = organization.getManagerGroups();

        Person personMember = get(Person.class, getPersonDn(userId));
        return personMember != null
                && personMember.getMemberOf().stream().anyMatch(m -> dns.stream().anyMatch(dn -> dn.equals(m)));

    }

    public String getIntrospectionEndpoint() {
        return jsonProperty(oxAuthConfDynamic, "introspectionEndpoint");
    }
    
    public String getAuthorizationEndpoint() {
        return jsonProperty(oxAuthConfDynamic, "authorizationEndpoint");
    }
    
    public String getTokenEndpoint() {
        return jsonProperty(oxAuthConfDynamic, "tokenEndpoint");
    }
    
    public String getUserInfoEndpoint() {
        return jsonProperty(oxAuthConfDynamic, "userInfoEndpoint");
    }
    
    public String getJwksUri() {
        return jsonProperty(oxAuthConfDynamic, "jwksUri");
    }

    @Produces
    @ApplicationScoped
    public StringEncrypter getStringEncrypter() {
        return stringEncrypter;
    }

    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    public DocumentStoreConfiguration getDocumentStoreConfiguration() {
        return documentStoreConfiguration;
    }

    public boolean isBackendLdapEnabled() {
        //TODO: remove dependency on this method
        return false;
    }

    public boolean authenticate(String uid, String pass) throws Exception {
        return entryManager.authenticate(rootDn, Person.class, uid, pass);
    }

    public void prepareFidoBranch(String userInum) {
        prepareBranch(userInum, "fido");
    }

    public void prepareFido2Branch(String userInum) {
        prepareBranch(userInum, "fido2_register");
    }

    public ApplicationConfiguration getAppConfiguration() {
        String baseDn = String.format("ou=casa,ou=configuration,%s", getRootDn());
        return get(ApplicationConfiguration.class, baseDn);
    }

    private void prepareBranch(String userInum, String ou) {

        String dn = String.format("ou=%s,%s", ou, getPersonDn(userInum));
        SimpleBranch entry = get(SimpleBranch.class, dn);
        if (entry == null) {
            logger.info("Non existing {} branch for {}, creating...", ou, userInum);
            entry = new SimpleBranch(dn, ou);

            if (!add(entry)) {
                logger.error("Could not create {} branch", ou);
            }
        }

    }

    public CustomScript getScript(String acr) {

        CustomScript script = new CustomScript();
        script.setDisplayName(acr);
        script.setBaseDn(getCustomScriptsDn());

        List<CustomScript> scripts = find(script);
        if (scripts.isEmpty()) {
            logger.warn("Script '{}' not found", acr);
            script = null;
        } else {
            script = scripts.get(0);
        }
        return script;

    }
    
    private String jsonProperty(JsonNode node, String ...path) {
        
        for (String prop : path) {
            node = node.get(prop);
        }
        return node.textValue();
        
    }

    private boolean loadApplianceSettings(Properties properties) {

        boolean success = false;
        try {
            loadOxAuthSettings(properties.getProperty("oxauth_ConfigurationEntryDN"));
            rootDn = "o=gluu";
            success = true;

            GluuConfiguration gluuConf = get(GluuConfiguration.class, jsonProperty(oxAuthConfStatic, "baseDn", "configuration"));
            cacheConfiguration = gluuConf.getCacheConfiguration();
            documentStoreConfiguration = gluuConf.getDocumentStoreConfiguration();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

    private void loadOxAuthSettings(String dn) throws Exception {

        oxAuthConfiguration conf = get(oxAuthConfiguration.class, dn);
        oxAuthConfDynamic = mapper.readTree(conf.getOxAuthConfDynamic());
        oxAuthConfStatic = mapper.readTree(conf.getOxAuthConfStatic());

        personCustomObjectClasses = Optional.ofNullable(oxAuthConfDynamic.get("personCustomObjectClassList"))
                .map(node -> {
                    try {
                        Set<String> ocs = new HashSet<>();
                        Iterator<JsonNode> it = node.elements();
                        while (it.hasNext()) {
                            ocs.add(it.next().asText());
                        }
                        return ocs;
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        return null;
                    }
                })
                .orElse(Collections.singleton("gluuCustomPerson"));

    }

    LdapOperationService getOperationService() {
        return ldapOperationService;
    }

    public String getRootDn() {
        return rootDn;
    }

    private boolean setup(int retries, int retry_interval) throws Exception {

        boolean ret = false;
        entryManager = null;
        stringEncrypter = Utils.stringEncrypter();

        //load the configuration using the oxcore-persistence-cdi API
        logger.debug("Obtaining PersistenceEntryManagerFactory from persistence API");
        PersistenceConfiguration persistenceConf = persistanceFactoryService.loadPersistenceConfiguration();
        FileConfiguration persistenceConfig = persistenceConf.getConfiguration();
        Properties backendProperties = persistenceConfig.getProperties();
        PersistenceEntryManagerFactory factory = persistanceFactoryService.getPersistenceEntryManagerFactory(persistenceConf);

        String type = factory.getPersistenceType();
        logger.info("Underlying database of type '{}' detected", type);
        String file = String.format("%s/%s", DEFAULT_CONF_BASE, persistenceConf.getFileName());
        logger.info("Using config file: {}", file);

        logger.debug("Decrypting backend properties");
        backendProperties = PropertiesDecrypter.decryptAllProperties(stringEncrypter, backendProperties);

        logger.info("Obtaining a Persistence EntryManager");
        int i = 0;

        do {
            try {
                i++;
                entryManager = factory.createEntryManager(backendProperties);
            } catch (Exception e) {
                logger.warn("Unable to create persistence entry manager, retrying in {} seconds", retry_interval);
                Thread.sleep(retry_interval * 1000);
            }
        } while (entryManager == null && i < retries);

        if (entryManager == null) {
            logger.error("No EntryManager could be obtained");
        } else {

            try (Reader f = new FileReader(String.format("%s/gluu.properties", DEFAULT_CONF_BASE))) {

                Properties generalProps = new Properties();
                generalProps.load(f);
                //Initialize important class members
                ret = loadApplianceSettings(generalProps);
            } catch (Exception e) {
                logger.error("Fatal: gluu.properties not readable", e);
            }
        }

        return ret;

    }

}
