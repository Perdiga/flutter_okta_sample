package io.crossingthestreams.flutteroktanative;

final class OktaSdkConstant {

    /** ======== Keys ======== **/

    static final String RESOLVE_TYPE_KEY = "resolve_type";

    static final String ACCESS_TOKEN_KEY = "access_token";

    static final String ID_TOKEN_KEY = "id_token";

    static final String REFRESH_TOKEN_KEY = "refresh_token";

    static final String AUTHENTICATED_KEY = "authenticated";

    static final String ERROR_CODE_KEY = "error_code";

    static final String ERROR_MSG_KEY = "error_message";

    static final String ACTIVE_KEY = "active";

    static final String TOKEN_TYPE_KEY = "token_type";

    static final String SCOPE_KEY = "scope";

    static final String CLIENT_ID_KEY = "client_id";

    static final String DEVICE_ID_KEY = "device_id";

    static final String USERNAME_KEY = "username";

    static final String NBF_KEY = "nbf";

    static final String EXP_KEY = "exp";

    static final String IAT_KEY = "iat";

    static final String SUB_KEY = "sub";

    static final String AUD_KEY = "aud";

    static final String ISS_KEY = "iss";

    static final String JTI_KEY = "jti";

    static final String UID_KEY = "uid";

    /** ======== Values ======== **/

    static final String AUTHORIZED = "authorized";

    static final String SIGNED_OUT = "signed_out";

    static final String CANCELLED = "cancelled";

    /** ======== Event names ======== **/

    static final String SIGN_IN_SUCCESS = "signInSuccess";

    static final String ON_ERROR = "onError";

    static final String SIGN_OUT_SUCCESS = "signOutSuccess";

    static final String ON_CANCELLED = "onCancelled";

    private OktaSdkConstant() {
        throw new AssertionError();
    }
}
