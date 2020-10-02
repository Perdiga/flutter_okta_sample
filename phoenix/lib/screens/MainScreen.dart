import 'package:flutter/material.dart';
import 'package:phoenix/providers/OktaProvider.dart';

class MainScreen extends StatelessWidget {
  static const routeName = '/main';

  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Container(
            child: RaisedButton(
          child: Text('Logout'),
          onPressed: () async {
            await AuthProvider.of(context).authService.logout();
            Navigator.of(context).pushReplacementNamed('/splash');
          },
        )),
      ),
    );
  }
}
