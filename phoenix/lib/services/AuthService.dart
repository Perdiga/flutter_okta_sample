import 'package:flutter_okta_sdk/BaseRequest.dart';
import 'package:flutter_okta_sdk/flutter_okta_sdk.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthOktaService {
  var oktaSdk = OktaSDK();
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

  static final OKTA_BASE_REQUEST = BaseRequest(
      clientId: OKTA_CLIENT_ID,
      discoveryUrl: OKTA_DISCOVERY_URL,
      endSessionRedirectUri: OKTA_LOGOUT_REDIRECT_URI,
      redirectUrl: OKTA_REDIRECT_URI,
      scopes: ['openid', 'profile', 'email', 'offline_access']);

  Future setup() async {
    await oktaSdk.setup(OKTA_BASE_REQUEST);
  }

  Future authorize() async {
    try {
      if (oktaSdk.isInitialized == false) {
        await this.setup();
      }
      await oktaSdk.signIn();
    } catch (e) {
      print(e);
    }
  }

  Future logout() async {
    try {
      if (oktaSdk.isInitialized == false) {
        await this.setup();
      }
      await oktaSdk.signOut();
    } catch (e) {
      print(e);
    }
  }
}
