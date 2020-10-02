import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_method_channel_for_okta_platform.dart';
import 'requests/BaseRequest.dart';

/* 
  Create an interface with the avaliable methods on flutter_okta_plugin
 */
abstract class FlutterOktaPlatform extends PlatformInterface {
  FlutterOktaPlatform() : super(token: _token);

  /// Creates an instance of [FlutterOktaPlatform] as [FlutterMethodChannelForOkta]
  static FlutterOktaPlatform get instance => _instance;
  static FlutterOktaPlatform _instance = FlutterMethodChannelForOktaPlatform();

  static final Object _token = Object();

  /// Platform-specific plugins should set this with their own platform-specific
  /// class that extends [FlutterOktaPlatform] when they register themselves.
  static set instance(FlutterOktaPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  // --------------------
  // Okta methods that can be implemented by channel
  // --------------------

  Future<void> signIn(BaseRequest request) async {
    throw UnimplementedError('signIn() has not been implemented');
  }

  Future<void> signOut(BaseRequest request) {
    throw UnimplementedError('signIn() has not been implemented');
  }
}
