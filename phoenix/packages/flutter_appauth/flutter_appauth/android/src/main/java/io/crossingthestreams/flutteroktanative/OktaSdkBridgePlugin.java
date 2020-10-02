package io.crossingthestreams.flutteroktanative;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.okta.oidc.AuthorizationStatus;
import com.okta.oidc.OIDCConfig;
import com.okta.oidc.Okta;
import com.okta.oidc.RequestCallback;
import com.okta.oidc.ResultCallback;
import com.okta.oidc.Tokens;
//import com.okta.oidc.results.Result;
import com.okta.oidc.clients.sessions.SessionClient;
import com.okta.oidc.clients.web.WebAuthClient;
import com.okta.oidc.clients.AuthClient;
import com.okta.oidc.net.params.TokenTypeHint;
import com.okta.oidc.net.response.IntrospectInfo;
import com.okta.oidc.net.response.UserInfo;
import com.okta.oidc.storage.SharedPreferenceStorage;
import com.okta.oidc.util.AuthorizationException;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class OktaSdkBridgePlugin implements FlutterPlugin, MethodCallHandler, PluginRegistry.ActivityResultListener, ActivityAware {
    private class OktaRequestParameters  {
        String clientId;
        String redirectUri;
        String endSessionRedirectUri;
        String discoveryUri;
        ArrayList<String> scopes;
        String userAgentTemplate;
        Boolean requireHardwareBackedKeyStore;
    }

    private class PendingOperation {
        final String method;
        final Result result;

        PendingOperation(String method, Result result) {
            this.method = method;
            this.result = result;
        }
    }

    private Context applicationContext;
    private Activity mainActivity;

    private OIDCConfig config;
    private WebAuthClient webClient;
    private AuthClient authClient;

    private PendingOperation pendingOperation;

    // Avaliable Methods
    private static final String SIGNOUT_METHOD = "signout";
    private static final String SIGNIN_METHOD = "signin";

    // Avaliable Methods Generic Errors
    private static final String SIGNOUT_ERROR_CODE = "signout_failed";
    private static final String SIGNIN_ERROR_CODE = "signin_failed";
    private static final String CREATECONFIG_ERROR_CODE = "createconfig_failed";
    private static final String SIGNIN_ACTIVITY_ERROR_CODE = "signin_activity_null";
    private static final String SIGNIN_CLIENT_ERROR_CODE = "signin_client_null";
    private static final String NOT_IMPLEMENTED_ERROR_CODE = "signin_client_null";





    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final OktaSdkBridgePlugin plugin = new OktaSdkBridgePlugin();
        plugin.setActivity(registrar.activity());
        plugin.onAttachedToEngine(registrar.context(), registrar.messenger());
        registrar.addActivityResultListener(plugin);
//        registrar.addViewDestroyListener(
//                new PluginRegistry.ViewDestroyListener() {
//                    @Override
//                    public boolean onViewDestroy(FlutterNativeView view) {
//                        return false;
//                    }
//                });
    }

    private void setActivity(Activity flutterActivity) {
        this.mainActivity = flutterActivity;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (pendingOperation == null) {
            return false;
        }
        return true;
    }

    private void onAttachedToEngine(Context context, BinaryMessenger binaryMessenger) {
        this.applicationContext = context;
        final MethodChannel channel = new MethodChannel(binaryMessenger, "crossingthestreams.io/flutter_appauth");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        binding.addActivityResultListener(this);
        mainActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.mainActivity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        binding.addActivityResultListener(this);
        mainActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.mainActivity = null;
    }

    // Avaliable Methods switch
    // This is the entrypoint for the bridge
    @Override
    public void onMethodCall(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();

        OktaRequestParameters oktaParams = processOktaRequestArguments(arguments);
        createConfig(oktaParams);

        switch (call.method) {
            case SIGNIN_METHOD:
                try {
                    checkAndSetPendingOperation(call.method, result);
                    handleSignInMethodCall();
                } catch (Exception ex) {
                    finishWithError(SIGNIN_ERROR_CODE, ex.getLocalizedMessage());
                }
                break;
            case SIGNOUT_METHOD:
                try {
                    checkAndSetPendingOperation(call.method, result);
                    handleSignOutMethodCall();
                } catch (Exception ex) {
                    finishWithError(SIGNOUT_ERROR_CODE, ex.getLocalizedMessage());
                }
                break;
            default:
                finishWithError(NOT_IMPLEMENTED_ERROR_CODE, "Method not implemented");
        }
    }

    // Configure Okta
    private void createConfig(OktaRequestParameters params) {
        try {
            String[] scopeArray = new String[params.scopes.size()];

            for (int i = 0; i < params.scopes.size(); i++) {
                scopeArray[i] = params.scopes.get(i);
            }

            this.config = new OIDCConfig.Builder()
                    .clientId(params.clientId)
                    .redirectUri(params.redirectUri)
                    .endSessionRedirectUri(params.endSessionRedirectUri)
                    .scopes(scopeArray)
                    .discoveryUri(params.discoveryUri)
                    .create();

            this.webClient = new Okta.WebAuthBuilder()
                    .withConfig(config)
                    .withContext(this.applicationContext)
                    .withStorage(new SharedPreferenceStorage(this.applicationContext))
                    .withOktaHttpClient(new HttpClientImpl(params.userAgentTemplate))
                    .setRequireHardwareBackedKeyStore(params.requireHardwareBackedKeyStore)
                    .create();

            this.authClient = new Okta.AuthBuilder()
                    .withConfig(config)
                    .withContext(this.applicationContext)
                    .withStorage(new SharedPreferenceStorage(this.applicationContext))
                    .withOktaHttpClient(new HttpClientImpl(params.userAgentTemplate))
                    .setRequireHardwareBackedKeyStore(params.requireHardwareBackedKeyStore)
                    .create();

        } catch (Exception ex) {
            finishWithError(CREATECONFIG_ERROR_CODE, ex.getLocalizedMessage());
        }
    }

    // Parse arguments that comes from flutter/dart to a nice java object
    @SuppressWarnings("unchecked")
    private OktaRequestParameters processOktaRequestArguments(Map<String, Object> arguments) {

        OktaRequestParameters okta = new OktaRequestParameters();
        okta.clientId = (String) arguments.get("clientId");
        okta.discoveryUri = (String) arguments.get("discoveryUrl");
        okta.endSessionRedirectUri = (String) arguments.get("endSessionRedirectUri");
        okta.redirectUri = (String) arguments.get("redirectUrl");
        okta.requireHardwareBackedKeyStore = (Boolean) arguments.get("requireHardwareBackedKeyStore");
        okta.scopes = (ArrayList<String>) arguments.get("scopes");
        okta.userAgentTemplate = (String) arguments.get("userAgentTemplate");

        return okta;
    }

    // Check if there is any operation pending
    private void checkAndSetPendingOperation(String method, Result result) {
        if (pendingOperation != null) {
            throw new IllegalStateException(
                    "Concurrent operations detected: " + pendingOperation.method + ", " + method);
        }
        pendingOperation = new PendingOperation(method, result);
    }

    private void finishWithError(String errorCode, String errorMessage) {
        if (pendingOperation != null) {
            pendingOperation.result.error(errorCode, errorMessage, null);
            pendingOperation = null;
        }
    }

    private void finishWithSuccess(Object data) {
        if (pendingOperation != null) {
            pendingOperation.result.success(data);
            pendingOperation = null;
        }
    }

    // Do the okta signin
    private void handleSignInMethodCall() {
        if (this.mainActivity == null) {
            finishWithError(SIGNIN_ACTIVITY_ERROR_CODE, "Activity not defined");
        }

        if (webClient == null) {
            finishWithError(SIGNIN_CLIENT_ERROR_CODE, "Web Client not defined");
        }

        webClient.signIn(this.mainActivity, null);
    }

    // Do the okta signout
    public void handleSignOutMethodCall() {
        if (this.mainActivity == null) {
            finishWithError(SIGNIN_ACTIVITY_ERROR_CODE, "Activity not defined");
        }

        if (webClient == null) {
            finishWithError(SIGNIN_CLIENT_ERROR_CODE, "Web Client not defined");
        }

        webClient.signOutOfOkta(this.mainActivity);
    }
}











//    @ReactMethod
//    public void signIn() {
//        Activity currentActivity = getCurrentActivity();
//
//        if (currentActivity == null) {
//            final WritableMap params = Arguments.createMap();
//            params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.NO_VIEW.getErrorCode());
//            params.putString(OktaSdkConstant.ERROR_MSG_KEY, OktaSdkError.NO_VIEW.getErrorMessage());
//            sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//            return;
//        }
//
//        if (webClient == null) {
//            final WritableMap params = Arguments.createMap();
//            params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.NOT_CONFIGURED.getErrorCode());
//            params.putString(OktaSdkConstant.ERROR_MSG_KEY, OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//            sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//            return;
//        }
//
//        webClient.signIn(currentActivity, null);
//    }
//
//    @ReactMethod
//    public void authenticate(String sessionToken,  final Promise promise) {
//        if (authClient == null) {
//            final WritableMap params = Arguments.createMap();
//            params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.NOT_CONFIGURED.getErrorCode());
//            params.putString(OktaSdkConstant.ERROR_MSG_KEY, OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//            sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//            promise.reject(OktaSdkError.NOT_CONFIGURED.getErrorCode(), OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//            return;
//        }
//
//        authClient.signIn(sessionToken, null, new RequestCallback<Result, AuthorizationException>() {
//            @Override
//            public void onSuccess(@NonNull Result result) {
//                if (result.isSuccess()) {
//                    try {
//                        SessionClient sessionClient = authClient.getSessionClient();
//                        Tokens tokens = sessionClient.getTokens();
//                        String token = tokens.getAccessToken();
//
//                        WritableMap params = Arguments.createMap();
//                        params.putString(OktaSdkConstant.RESOLVE_TYPE_KEY, OktaSdkConstant.AUTHORIZED);
//                        params.putString(OktaSdkConstant.ACCESS_TOKEN_KEY, token);
//                        sendEvent(reactContext, OktaSdkConstant.SIGN_IN_SUCCESS, params);
//
//                        params = Arguments.createMap();
//                        params.putString(OktaSdkConstant.RESOLVE_TYPE_KEY, OktaSdkConstant.AUTHORIZED);
//                        params.putString(OktaSdkConstant.ACCESS_TOKEN_KEY, token);
//                        promise.resolve(params);
//                    } catch (AuthorizationException e) {
//                        WritableMap params = Arguments.createMap();
//                        params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.SIGN_IN_FAILED.getErrorCode());
//                        params.putString(OktaSdkConstant.ERROR_MSG_KEY, OktaSdkError.SIGN_IN_FAILED.getErrorMessage());
//                        sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//                        promise.reject(OktaSdkError.SIGN_IN_FAILED.getErrorCode(), OktaSdkError.SIGN_IN_FAILED.getErrorMessage());
//                    }
//                } else {
//                    WritableMap params = Arguments.createMap();
//                    params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.SIGN_IN_FAILED.getErrorCode());
//                    params.putString(OktaSdkConstant.ERROR_MSG_KEY, OktaSdkError.SIGN_IN_FAILED.getErrorMessage());
//                    sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//                    promise.reject(OktaSdkError.SIGN_IN_FAILED.getErrorCode(), OktaSdkError.SIGN_IN_FAILED.getErrorMessage());
//                }
//            }
//
//            @Override
//            public void onError(String error, AuthorizationException exception) {
//                WritableMap params = Arguments.createMap();
//                params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.OKTA_OIDC_ERROR.getErrorCode());
//                params.putString(OktaSdkConstant.ERROR_MSG_KEY, error);
//                sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//                promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), OktaSdkError.OKTA_OIDC_ERROR.getErrorMessage());
//            }
//        });
//    }
//
//    @ReactMethod
//    public void getAccessToken(final Promise promise) {
//        try {
//            if (webClient == null) {
//                promise.reject(OktaSdkError.NOT_CONFIGURED.getErrorCode(), OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//                return;
//            }
//
//            final WritableMap params = Arguments.createMap();
//            final SessionClient sessionClient = webClient.getSessionClient();
//            final Tokens tokens = sessionClient.getTokens();
//
//            if (tokens.getAccessToken() == null) {
//                promise.reject(OktaSdkError.NO_ACCESS_TOKEN.getErrorCode(), OktaSdkError.NO_ACCESS_TOKEN.getErrorMessage());
//                return;
//            }
//
//            params.putString(OktaSdkConstant.ACCESS_TOKEN_KEY, tokens.getAccessToken());
//            promise.resolve(params);
//
//        } catch (Exception e) {
//            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), e.getLocalizedMessage(), e);
//        }
//    }
//
//    @ReactMethod
//    public void getIdToken(Promise promise) {
//        try {
//            if (webClient == null) {
//                promise.reject(OktaSdkError.NOT_CONFIGURED.getErrorCode(), OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//                return;
//            }
//
//            final WritableMap params = Arguments.createMap();
//            SessionClient sessionClient = webClient.getSessionClient();
//            Tokens tokens = sessionClient.getTokens();
//            String idToken = tokens.getIdToken();
//            if (idToken != null) {
//                params.putString(OktaSdkConstant.ID_TOKEN_KEY, idToken);
//                promise.resolve(params);
//            } else {
//                promise.reject(OktaSdkError.NO_ID_TOKEN.getErrorCode(), OktaSdkError.NO_ID_TOKEN.getErrorMessage());
//            }
//        } catch (Exception e) {
//            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), e.getLocalizedMessage(), e);
//        }
//    }
//
//    @ReactMethod
//    public void getUser(final Promise promise) {
//        if (webClient == null) {
//            promise.reject(OktaSdkError.NOT_CONFIGURED.getErrorCode(), OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//            return;
//        }
//
//        SessionClient sessionClient = webClient.getSessionClient();
//        sessionClient.getUserProfile(new RequestCallback<UserInfo, AuthorizationException>() {
//            @Override
//            public void onSuccess(@NonNull UserInfo result) {
//                promise.resolve(result.toString());
//            }
//
//            @Override
//            public void onError(String msg, AuthorizationException error) {
//                promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), error.getLocalizedMessage(), error);
//            }
//        });
//    }
//
//    @ReactMethod
//    public void isAuthenticated(Promise promise) {
//        try {
//            if (webClient == null) {
//                promise.reject(OktaSdkError.NOT_CONFIGURED.getErrorCode(), OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//                return;
//            }
//
//            final WritableMap params = Arguments.createMap();
//            SessionClient sessionClient = webClient.getSessionClient();
//            if (sessionClient.isAuthenticated()) {
//                params.putBoolean(OktaSdkConstant.AUTHENTICATED_KEY, true);
//            } else {
//                params.putBoolean(OktaSdkConstant.AUTHENTICATED_KEY, false);
//            }
//            promise.resolve(params);
//        } catch (Exception e) {
//            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), e.getLocalizedMessage(), e);
//        }
//    }
//
//    @ReactMethod
//    public void signOut() {
//        Activity currentActivity = getCurrentActivity();
//
//        if (currentActivity == null) {
//            final WritableMap params = Arguments.createMap();
//            params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.NO_VIEW.getErrorCode());
//            params.putString(OktaSdkConstant.ERROR_MSG_KEY, OktaSdkError.NO_VIEW.getErrorMessage());
//            sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//            return;
//        }
//
//        if (webClient == null) {
//            final WritableMap params = Arguments.createMap();
//            params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.NOT_CONFIGURED.getErrorCode());
//            params.putString(OktaSdkConstant.ERROR_MSG_KEY, OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//            sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//            return;
//        }
//
//        webClient.signOutOfOkta(currentActivity);
//    }
//
//    @ReactMethod
//    public void revokeAccessToken(Promise promise) {
//        revokeToken(TokenTypeHint.ACCESS_TOKEN, promise);
//    }
//
//    @ReactMethod
//    public void revokeIdToken(Promise promise) {
//        revokeToken(TokenTypeHint.ID_TOKEN, promise);
//    }
//
//    @ReactMethod
//    public void revokeRefreshToken(Promise promise) {
//        revokeToken(TokenTypeHint.REFRESH_TOKEN, promise);
//    }
//
//    @ReactMethod
//    public void introspectAccessToken(Promise promise) {
//        introspectToken(TokenTypeHint.ACCESS_TOKEN, promise);
//    }
//
//    @ReactMethod
//    public void introspectIdToken(Promise promise) {
//        introspectToken(TokenTypeHint.ID_TOKEN, promise);
//    }
//
//    @ReactMethod
//    public void introspectRefreshToken(Promise promise) {
//        introspectToken(TokenTypeHint.REFRESH_TOKEN, promise);
//    }
//
//    @ReactMethod
//    public void refreshTokens(final Promise promise) {
//        try {
//
//            if (webClient == null) {
//                promise.reject(OktaSdkError.NOT_CONFIGURED.getErrorCode(), OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//                return;
//            }
//
//            webClient.getSessionClient().refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
//                @Override
//                public void onSuccess(@NonNull Tokens result) {
//                    WritableMap params = Arguments.createMap();
//                    params.putString(OktaSdkConstant.ACCESS_TOKEN_KEY, result.getAccessToken());
//                    params.putString(OktaSdkConstant.ID_TOKEN_KEY, result.getIdToken());
//                    params.putString(OktaSdkConstant.REFRESH_TOKEN_KEY, result.getRefreshToken());
//                    promise.resolve(params);
//                }
//
//                @Override
//                public void onError(String e, AuthorizationException error) {
//                    promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), error.getLocalizedMessage(), error);
//                }
//            });
//        } catch (Error e) {
//            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), e.getLocalizedMessage(), e);
//        }
//    }
//
//    @ReactMethod
//    public void clearTokens(final Promise promise) {
//        try {
//            if (webClient != null) {
//                webClient.getSessionClient().clear();
//            }
//
//            if (authClient != null) {
//                authClient.getSessionClient().clear();
//            }
//            promise.resolve(true);
//        } catch (Exception e) {
//            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), e.getLocalizedMessage(), e);
//        }
//    }
//
//    @Override
//    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
//        webClient.handleActivityResult(requestCode & 0xffff, resultCode, data);
//        Activity currentActivity = getCurrentActivity();
//        registerCallback(currentActivity);
//    }
//
//    @Override
//    public void onNewIntent(Intent intent) {
//
//    }
//
//    /** ================= Private Methods ================= **/
//
//    private void sendEvent(ReactContext reactContext,
//                           String eventName,
//                           @Nullable WritableMap params) {
//        reactContext
//                .getJSModule(RCTNativeAppEventEmitter.class)
//                .emit(eventName, params);
//    }
//
//    private void registerCallback(Activity activity) {
//        final SessionClient sessionClient = webClient.getSessionClient();
//
//        webClient.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
//            @Override
//            public void onSuccess(@NonNull AuthorizationStatus status) {
//                if (status == AuthorizationStatus.AUTHORIZED) {
//                    try {
//                        WritableMap params = Arguments.createMap();
//                        Tokens tokens = sessionClient.getTokens();
//                        params.putString(OktaSdkConstant.RESOLVE_TYPE_KEY, OktaSdkConstant.AUTHORIZED);
//                        params.putString(OktaSdkConstant.ACCESS_TOKEN_KEY, tokens.getAccessToken());
//                        sendEvent(reactContext, OktaSdkConstant.SIGN_IN_SUCCESS, params);
//                    } catch (AuthorizationException e) {
//                        WritableMap params = Arguments.createMap();
//                        params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.SIGN_IN_FAILED.getErrorCode());
//                        params.putString(OktaSdkConstant.ERROR_MSG_KEY, OktaSdkError.SIGN_IN_FAILED.getErrorMessage());
//                        sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//                    }
//                } else if (status == AuthorizationStatus.SIGNED_OUT) {
//                    sessionClient.clear();
//                    WritableMap params = Arguments.createMap();
//                    params.putString(OktaSdkConstant.RESOLVE_TYPE_KEY, OktaSdkConstant.SIGNED_OUT);
//                    sendEvent(reactContext, OktaSdkConstant.SIGN_OUT_SUCCESS, params);
//                }
//            }
//
//            @Override
//            public void onCancel() {
//                WritableMap params = Arguments.createMap();
//                params.putString(OktaSdkConstant.RESOLVE_TYPE_KEY, OktaSdkConstant.CANCELLED);
//                sendEvent(reactContext, OktaSdkConstant.ON_CANCELLED, params);
//            }
//
//            @Override
//            public void onError(@NonNull String msg, AuthorizationException error) {
//                WritableMap params = Arguments.createMap();
//                params.putString(OktaSdkConstant.ERROR_CODE_KEY, OktaSdkError.OKTA_OIDC_ERROR.getErrorCode());
//                params.putString(OktaSdkConstant.ERROR_MSG_KEY, msg);
//                sendEvent(reactContext, OktaSdkConstant.ON_ERROR, params);
//            }
//        }, activity);
//    }
//
//    private void revokeToken(String tokenName, final Promise promise) {
//        try {
//            if (webClient == null) {
//                promise.reject(OktaSdkError.NOT_CONFIGURED.getErrorCode(), OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//                return;
//            }
//
//            final SessionClient sessionClient = webClient.getSessionClient();
//            Tokens tokens = sessionClient.getTokens();
//            String token;
//
//            switch (tokenName) {
//                case TokenTypeHint.ACCESS_TOKEN:
//                    token = tokens.getAccessToken();
//                    break;
//                case TokenTypeHint.ID_TOKEN:
//                    token = tokens.getIdToken();
//                    break;
//                case TokenTypeHint.REFRESH_TOKEN:
//                    token = tokens.getRefreshToken();
//                    break;
//                default:
//                    promise.reject(OktaSdkError.ERROR_TOKEN_TYPE.getErrorCode(), OktaSdkError.ERROR_TOKEN_TYPE.getErrorMessage());
//                    return;
//            }
//
//            sessionClient.revokeToken(token,
//                    new RequestCallback<Boolean, AuthorizationException>() {
//                        @Override
//                        public void onSuccess(@NonNull Boolean result) {
//                            promise.resolve(result);
//                        }
//                        @Override
//                        public void onError(String msg, AuthorizationException error) {
//                            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), error.getLocalizedMessage(), error);
//                        }
//                    });
//        } catch (Exception e) {
//            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), e.getLocalizedMessage(), e);
//        }
//    }
//
//    private void introspectToken(String tokenName, final Promise promise) {
//        try {
//            if (webClient == null) {
//                promise.reject(OktaSdkError.NOT_CONFIGURED.getErrorCode(), OktaSdkError.NOT_CONFIGURED.getErrorMessage());
//            }
//
//            final SessionClient sessionClient = webClient.getSessionClient();
//            Tokens tokens = sessionClient.getTokens();
//            String token;
//
//            switch (tokenName) {
//                case TokenTypeHint.ACCESS_TOKEN:
//                    token = tokens.getAccessToken();
//                    break;
//                case TokenTypeHint.ID_TOKEN:
//                    token = tokens.getIdToken();
//                    break;
//                case TokenTypeHint.REFRESH_TOKEN:
//                    token = tokens.getRefreshToken();
//                    break;
//                default:
//                    promise.reject(OktaSdkError.ERROR_TOKEN_TYPE.getErrorCode(), OktaSdkError.ERROR_TOKEN_TYPE.getErrorMessage());
//                    return;
//            }
//
//            webClient.getSessionClient().introspectToken(token,
//                    tokenName, new RequestCallback<IntrospectInfo, AuthorizationException>() {
//                        @Override
//                        public void onSuccess(@NonNull IntrospectInfo result) {
//                            WritableMap params = Arguments.createMap();
//                            params.putBoolean(OktaSdkConstant.ACTIVE_KEY, result.isActive());
//                            params.putString(OktaSdkConstant.TOKEN_TYPE_KEY, result.getTokenType());
//                            params.putString(OktaSdkConstant.SCOPE_KEY, result.getScope());
//                            params.putString(OktaSdkConstant.CLIENT_ID_KEY, result.getClientId());
//                            params.putString(OktaSdkConstant.DEVICE_ID_KEY, result.getDeviceId());
//                            params.putString(OktaSdkConstant.USERNAME_KEY, result.getUsername());
//                            params.putInt(OktaSdkConstant.NBF_KEY, result.getNbf());
//                            params.putInt(OktaSdkConstant.EXP_KEY, result.getExp());
//                            params.putInt(OktaSdkConstant.IAT_KEY, result.getIat());
//                            params.putString(OktaSdkConstant.SUB_KEY, result.getSub());
//                            params.putString(OktaSdkConstant.AUD_KEY, result.getAud());
//                            params.putString(OktaSdkConstant.ISS_KEY, result.getIss());
//                            params.putString(OktaSdkConstant.JTI_KEY, result.getJti());
//                            params.putString(OktaSdkConstant.UID_KEY, result.getUid());
//                            promise.resolve(params);
//                        }
//
//                        @Override
//                        public void onError(String e, AuthorizationException error) {
//                            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), error.getLocalizedMessage(), error);
//                        }
//                    }
//            );
//        } catch (AuthorizationException e) {
//            promise.reject(OktaSdkError.OKTA_OIDC_ERROR.getErrorCode(), e.getLocalizedMessage(), e);
//        }
//    }
//}
