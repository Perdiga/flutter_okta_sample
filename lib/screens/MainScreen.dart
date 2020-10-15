import 'package:flutter/material.dart';
import 'package:phoenix/providers/OktaProvider.dart';
import 'package:fluttertoast/fluttertoast.dart';

import 'dart:convert';

class MainScreen extends StatelessWidget {
  static const routeName = '/main';

  parseUser(Map<String, dynamic> json) {
    return json['name'] as String;
  }

  Widget build(BuildContext context) {
    return Scaffold(
      body: SingleChildScrollView(
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              RaisedButton(
                onPressed: () async {
                  var userJson =
                      await AuthProvider.of(context).authService.getUser();
                  print(userJson);
                  Map<String, dynamic> user = jsonDecode(userJson);

                  Fluttertoast.showToast(msg: "Usuário: ${user['name']}");
                },
                child: const Text('GetUser', style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  await AuthProvider.of(context).authService.logout();
                  Navigator.of(context).pushReplacementNamed('/splash');
                },
                child: const Text('Logout', style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var isAuthenticated = await AuthProvider.of(context)
                      .authService
                      .isAuthenticated();
                  Fluttertoast.showToast(
                      msg: "isAuthenticated: ${isAuthenticated.toString()}");
                },
                child: const Text('IsAuthenticated',
                    style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var accessToken = await AuthProvider.of(context)
                      .authService
                      .getAccessToken();
                  Fluttertoast.showToast(msg: "AccessToken: $accessToken");
                },
                child: const Text('GetAccessToken',
                    style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var idToken =
                      await AuthProvider.of(context).authService.getIdToken();
                  Fluttertoast.showToast(msg: "idToken: $idToken");
                },
                child: const Text('GetIdToken', style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var result = await AuthProvider.of(context)
                      .authService
                      .revokeAccessToken();
                  Fluttertoast.showToast(msg: "result: $result");
                  Navigator.of(context).pushReplacementNamed('/splash');
                },
                child: const Text('RevokeAccessToken',
                    style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var result = await AuthProvider.of(context)
                      .authService
                      .revokeIdToken();
                  Fluttertoast.showToast(msg: "result: $result");
                  Navigator.of(context).pushReplacementNamed('/splash');
                },
                child:
                    const Text('RevokeIdToken', style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var result = await AuthProvider.of(context)
                      .authService
                      .revokeRefreshToken();
                  Fluttertoast.showToast(msg: "result: $result");
                  Navigator.of(context).pushReplacementNamed('/splash');
                },
                child: const Text('RevokeRefreshToken',
                    style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var result =
                      await AuthProvider.of(context).authService.clearTokens();
                  Fluttertoast.showToast(msg: "result: $result");
                  Navigator.of(context).pushReplacementNamed('/splash');
                },
                child:
                    const Text('ClearTokens', style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var result = await AuthProvider.of(context)
                      .authService
                      .introspectAccessToken();
                  Fluttertoast.showToast(msg: "introspectAccessToken: $result");
                },
                child: const Text('IntrospectAccessToken',
                    style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var result = await AuthProvider.of(context)
                      .authService
                      .introspectIdToken();
                  Fluttertoast.showToast(msg: "introspectIdToken: $result");
                },
                child: const Text('introspectIdToken',
                    style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var result = await AuthProvider.of(context)
                      .authService
                      .introspectRefreshToken();
                  Fluttertoast.showToast(
                      msg: "introspectRefreshToken: $result");
                },
                child: const Text('introspectRefreshToken',
                    style: TextStyle(fontSize: 20)),
              ),
              const SizedBox(height: 30),
              RaisedButton(
                onPressed: () async {
                  var result = await AuthProvider.of(context)
                      .authService
                      .refreshTokens();
                  Fluttertoast.showToast(msg: "refreshTokens: $result");
                },
                child:
                    const Text('refreshTokens', style: TextStyle(fontSize: 20)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}