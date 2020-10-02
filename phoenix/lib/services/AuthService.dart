import 'package:flutter_appauth/flutter_appauth.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthOktaService {
  FlutterAppAuth appAuth = FlutterAppAuth();
  FlutterSecureStorage secureStorage = FlutterSecureStorage();

  static const String OKTA_DOMAIN = 'dev-590808.okta.com';
  static const String OKTA_AUTHORIZER = 'default';
  static const String OKTA_CLIENT_ID = '0oa11v7wzcWEjmK4u4x7';

  static const String OKTA_ISSUER_URL =
      'https://$OKTA_DOMAIN/oauth2/$OKTA_AUTHORIZER';
  static const String OKTA_DISCOVERY_URL =
      'https://$OKTA_DOMAIN/.well-known/openid-configuration';

  static const String OKTA_REDIRECT_URI = 'com.deere.phoenix:/callback';
  static const String OKTA_LOGOUT_REDIRECT_URI = 'com.deere.phoenix:/splash';

  Future authorize() async {
    try {
      final AuthorizationTokenResponse result =
          await appAuth.authorizeAndExchangeCode(
        AuthorizationTokenRequest(
          OKTA_CLIENT_ID,
          OKTA_REDIRECT_URI,
          issuer: OKTA_ISSUER_URL,
          scopes: ['openid', 'profile', 'email', 'offline_access'],
        ),
      );

      await storeToken(result.idToken, result.accessToken, result.refreshToken);
      return result.accessToken;
    } catch (e) {
      print(e);
    }
  }

  Future logout() async {
    try {
      final String storedIdToken = await secureStorage.read(key: 'id_token');

      final AuthorizationTokenResponse result =
          await appAuth.authorizeAndExchangeCode(
        AuthorizationTokenRequest(
          OKTA_CLIENT_ID,
          OKTA_LOGOUT_REDIRECT_URI,
          issuer: OKTA_ISSUER_URL,
          scopes: ['openid', 'profile', 'email', 'offline_access'],
          additionalParameters: {
            "id_token_hint": storedIdToken,
            "post_logout_redirect_uri": OKTA_LOGOUT_REDIRECT_URI
          },
          serviceConfiguration: AuthorizationServiceConfiguration(
              "$OKTA_ISSUER_URL/v1/logout", "$OKTA_ISSUER_URL/v1/token"),
        ),
      );

      await storeToken(result.idToken, result.accessToken, result.refreshToken);
      return result.accessToken;
    } catch (e) {
      print(e);
    }
  }

  Future<void> storeToken(
      String idToken, String accessToken, String refreshToken) async {
    if (idToken != null) {
      print("idToken >>> $idToken");
      await secureStorage.write(key: 'id_token', value: idToken);
    }
    if (accessToken != null) {
      print("accessToken >>> $accessToken");
      await secureStorage.write(key: 'access_token', value: accessToken);
    }
    if (refreshToken != null) {
      print("refreshToken >>> $refreshToken");
      await secureStorage.write(key: 'refresh_token', value: refreshToken);
    }
  }
}
