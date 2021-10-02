import React, { useEffect } from 'react'
import {
  Form,
  Button,
  FormGroup,
  Col,
  CustomInput,
} from '../../../../../app/components'
import GluuLabel from '../../../../../app/routes/Apps/Gluu/GluuLabel'
import GluuCheckBoxRow from '../../../../../app/routes/Apps/Gluu/GluuCheckBoxRow'
import GluuLoader from '../../../../../app/routes/Apps/Gluu/GluuLoader'
import GluuViewWrapper from '../../../../../app/routes/Apps/Gluu/GluuViewWrapper'
import { connect } from 'react-redux'
import { Formik } from 'formik'
import {
  getLoggingConfig,
  editLoggingConfig,
} from '../../../redux/actions/LoggingActions'
import {
  hasPermission,
  LOGGING_READ,
  LOGGING_WRITE,
} from '../../../../../app/utils/PermChecker'
import { useTranslation } from 'react-i18next'

function LoggingPage({ logging, dispatch, permissions, loading }) {
  const { t } = useTranslation()
  useEffect(() => {
    dispatch(getLoggingConfig())
  }, [])

  const initialValues = {
    loggingLevel: logging.loggingLevel,
    loggingLayout: logging.loggingLayout,
    httpLoggingEnabled: logging.httpLoggingEnabled,
    disableJdkLogger: logging.disableJdkLogger,
    enabledOAuthAuditLogging: logging.enabledOAuthAuditLogging,
  }
  const levels = ['TRACE', 'DEBUG', 'INFO', 'ERROR']
  const logLayouts = ['text', 'json']
  return (
    <GluuLoader blocking={loading}>
      <GluuViewWrapper canShow={hasPermission(permissions, LOGGING_READ)}>
        <Formik
          initialValues={initialValues}
          onSubmit={(values) => {
            values.httpLoggingEnabled =
              values.httpLoggingEnabled != undefined
                ? values.httpLoggingEnabled
                : logging.httpLoggingEnabled
            values.disableJdkLogger =
              values.disableJdkLogger != undefined
                ? values.disableJdkLogger
                : logging.disableJdkLogger
            values.enabledOAuthAuditLogging =
              values.enabledOAuthAuditLogging != undefined
                ? values.enabledOAuthAuditLogging
                : logging.enabledOAuthAuditLogging

            const opts = {}
            opts['loggingConfiguration'] = JSON.stringify(values)
            dispatch(editLoggingConfig(opts))
          }}
        >
          {(formik) => (
            <Form onSubmit={formik.handleSubmit}>
              <FormGroup row></FormGroup>
              <FormGroup row>
                <GluuLabel label="fields.log_level" size={4} />
                <Col sm={8}>
                  <CustomInput
                    type="select"
                    id="loggingLevel"
                    name="loggingLevel"
                    value={logging.loggingLevel}
                    onChange={(e) => {
                      logging.loggingLevel = e.target.value
                      formik.setFieldValue('loggingLevel', e.target.value)
                    }}
                  >
                    <option value="">{t('actions.choose')}...</option>
                    {levels.map((item, key) => (
                      <option value={item} key={key}>
                        {item}
                      </option>
                    ))}
                  </CustomInput>
                </Col>
              </FormGroup>
              <FormGroup row>
                <GluuLabel label="fields.log_layout" size={4} />
                <Col sm={8}>
                  <CustomInput
                    type="select"
                    id="loggingLayout"
                    name="loggingLayout"
                    value={logging.loggingLayout}
                    onChange={(e) => {
                      logging.loggingLayout = e.target.value
                      formik.setFieldValue('loggingLayout', e.target.value)
                    }}
                  >
                    <option value="">{t('actions.choose')}...</option>
                    {logLayouts.map((item, key) => (
                      <option value={item} key={key}>
                        {item}
                      </option>
                    ))}
                  </CustomInput>
                </Col>
              </FormGroup>
              <GluuCheckBoxRow
                label="fields.http_logging_enabled"
                name="httpLoggingEnabled"
                handleOnChange={(e) => {
                  formik.setFieldValue('httpLoggingEnabled', e.target.checked)
                }}
                lsize={5}
                rsize={7}
                value={logging.httpLoggingEnabled}
              ></GluuCheckBoxRow>
              <GluuCheckBoxRow
                label="fields.disable_jdk_logger"
                name="disableJdkLogger"
                handleOnChange={(e) => {
                  formik.setFieldValue('disableJdkLogger', e.target.checked)
                }}
                lsize={5}
                rsize={7}
                value={logging.disableJdkLogger}
              ></GluuCheckBoxRow>
              <GluuCheckBoxRow
                label="fields.enabled_oAuth_audit_logging"
                name="enabledOAuthAuditLogging"
                handleOnChange={(e) => {
                  formik.setFieldValue(
                    'enabledOAuthAuditLogging',
                    e.target.checked,
                  )
                }}
                lsize={5}
                rsize={7}
                value={logging.enabledOAuthAuditLogging}
              ></GluuCheckBoxRow>

              {hasPermission(permissions, LOGGING_WRITE) && (
                <Button color="primary" type="submit">
                  {t('actions.save')}
                </Button>
              )}
            </Form>
          )}
        </Formik>
      </GluuViewWrapper>
    </GluuLoader>
  )
}
const mapStateToProps = (state) => {
  return {
    logging: state.loggingReducer.logging,
    loading: state.loggingReducer.loading,
    permissions: state.authReducer.permissions,
  }
}
export default connect(mapStateToProps)(LoggingPage)
