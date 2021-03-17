/**
 * Openid Client Sagas
 */
import { call, all, put, fork, select, takeLatest } from 'redux-saga/effects'
import { getOpenidClientsResponse } from '../actions/OpenidClientActions'
import { getAPIAccessToken } from '../actions/AuthActions'
import { GET_OPENID_CLIENTS } from '../actions/types'
import OIDCApi from '../api/OIDCApi'
import { getClient } from '../api/base'
import { isFourZeroOneError } from '../../utils/TokenController'
const JansConfigApi = require('jans_config_api')

function* newFunction() {
  const token = yield select((state) => state.authReducer.token.access_token)
  const issuer = yield select((state) => state.authReducer.issuer)
  const api = new JansConfigApi.OAuthOpenIDConnectClientsApi(
    getClient(JansConfigApi, token, issuer),
  )
  return new OIDCApi(api)
}

export function* getOauthOpenidClients() {
  try {
    const openIdApi = yield* newFunction()
    const data = yield call(openIdApi.getAllOpenidClients)
    yield put(getOpenidClientsResponse(data))
  } catch (e) {
    yield put(getOpenidClientsResponse(null))
    if (isFourZeroOneError(e)) {
      const jwt = yield select((state) => state.authReducer.userinfo_jwt)
      yield put(getAPIAccessToken(jwt))
    }
  }
}

export function* watchGetOpenidClients() {
  yield takeLatest(GET_OPENID_CLIENTS, getOauthOpenidClients)
}

export default function* rootSaga() {
  yield all([fork(watchGetOpenidClients)])
}
