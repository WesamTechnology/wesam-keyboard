import 'package:flutter/material.dart';
import 'package:android_intent_plus/android_intent.dart';
import 'package:android_intent_plus/flag.dart';
import 'package:keybord/pages/about.dart';
import 'package:keybord/pages/home.dart';
import 'package:keybord/pages/verify_code_page.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

import 'core/activation_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Supabase.initialize(
    url: 'https://ufzpxmhqwohbqmrffael.supabase.co',
    anonKey: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVmenB4bWhxd29oYnFtcmZmYWVsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjAyMDQ1MTMsImV4cCI6MjA3NTc4MDUxM30.NZ2Evck6Xd8yzYxCcSTrKE-IIavhr0cHnqd3fMJKU5U',
  );
  final isActivated = await ActivationService.isActivated();
  runApp(MyKeyboardApp(isActivated: isActivated));
}

class MyKeyboardApp extends StatefulWidget {
  final bool isActivated;
  const MyKeyboardApp({super.key, required this.isActivated});

  @override
  State<MyKeyboardApp> createState() => _MyKeyboardAppState();
}

class _MyKeyboardAppState extends State<MyKeyboardApp> {

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.blue,
        fontFamily: 'Arial',
      ),
      home:  widget.isActivated ? Home() : VerifyCodePage(),
    );
  }
}
