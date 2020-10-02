import 'package:flutter/services.dart';
import 'flutter_okta_platform.dart';
import 'requests/BaseRequest.dart';

//Todo: Change to okta
const MethodChannel _channel =
    MethodChannel('crossingthestreams.io/flutter_appauth');

class FlutterMethodChannelForOktaPlatform extends FlutterOktaPlatform {
  @override
  Future<void> signIn(BaseRequest request) async {
    await _channel.invokeMethod('signIn', convertBaseRequestToMap(request));
  }

  @override
  Future<void> signOut(BaseRequest request) async {
    await _channel.invokeMethod('signOut', convertBaseRequestToMap(request));
  }
}
