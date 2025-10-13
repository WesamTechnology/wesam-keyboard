import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:flutter_otp_text_field/flutter_otp_text_field.dart';
import 'package:url_launcher/url_launcher.dart';
import '../core/activation_service.dart';
import 'home.dart'; // صفحة الكيبورد بعد التفعيل

class VerifyCodePage extends StatefulWidget {
  const VerifyCodePage({super.key});

  @override
  State<VerifyCodePage> createState() => _VerifyCodePageState();
}

class _VerifyCodePageState extends State<VerifyCodePage> {
  String _enteredCode = "";
  bool _loading = false;
  String? _message;

  Future<void> _verifyCode() async {
    if (_enteredCode.isEmpty || _enteredCode.length < 6) {
      setState(() => _message = "⚠️ الرجاء إدخال الكود الكامل");
      return;
    }

    setState(() {
      _loading = true;
      _message = null;
    });

    final supabase = Supabase.instance.client;

    final response = await supabase
        .from('one_time_codes')
        .select()
        .eq('code', _enteredCode)
        .eq('used', false)
        .maybeSingle();

    if (response == null) {
      setState(() {
        _message = "❌ الكود غير صالح أو تم استخدامه.";
        _loading = false;
      });
      return;
    }

    await supabase
        .from('one_time_codes')
        .update({'used': true})
        .eq('code', _enteredCode);

    await ActivationService.setActivated();

    setState(() {
      _message = "✅ تم التفعيل بنجاح! جاري فتح التطبيق...";
      _loading = false;
    });

    Future.delayed(const Duration(seconds: 2), () {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const Home()),
      );
    });
  }

  Future<void> _launchWhatsApp() async {
    final Uri whatsapp = Uri.parse('https://wa.me/967775904988');
    if (await canLaunchUrl(whatsapp)) {
      await launchUrl(whatsapp, mode: LaunchMode.externalApplication);
    }
  }

  @override
  Widget build(BuildContext context) {
    final Color mainColor = Colors.blueAccent;

    return Scaffold(
      backgroundColor: Colors.grey.shade100,
      resizeToAvoidBottomInset: true, // ✅ يجعل الصفحة ترفع عند الكيبورد
      appBar: AppBar(
        title: const Text("تفعيل الكود"),
        backgroundColor: mainColor,
        elevation: 0,
        centerTitle: true,
      ),

      // ✅ نستخدم SingleChildScrollView لتجنب overflow عند ظهور الكيبورد
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            return SingleChildScrollView(
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: constraints.maxHeight),
                child: IntrinsicHeight(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      const SizedBox(height: 40),
                      const Icon(Icons.verified_user,
                          size: 80, color: Colors.blueAccent),
                      const SizedBox(height: 20),
                      const Text(
                        "أدخل كود التفعيل المكون من 6 أرقام الذي استلمته عبر واتساب",
                        style: TextStyle(
                            fontSize: 16, fontWeight: FontWeight.w500),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 40),

                      // 🔢 مربعات إدخال الكود
                      OtpTextField(
                        numberOfFields: 6,
                        borderColor: mainColor,
                        focusedBorderColor: mainColor,
                        showFieldAsBox: true,
                        borderRadius: BorderRadius.circular(10),
                        fieldWidth: 45,
                        filled: true,
                        fillColor: Colors.white,
                        onSubmit: (value) {
                          _enteredCode = value;
                        },
                        onCodeChanged: (value) {
                          _enteredCode = value;
                        },
                      ),

                      const SizedBox(height: 40),

                      // زر التأكيد
                      ElevatedButton(
                        onPressed: _loading ? null : _verifyCode,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: mainColor,
                          minimumSize: const Size.fromHeight(55),
                          shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12)),
                        ),
                        child: _loading
                            ? const CircularProgressIndicator(
                            color: Colors.white)
                            : const Text(
                          "تأكيد الكود",
                          style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold),
                        ),
                      ),

                      const SizedBox(height: 25),

                      if (_message != null)
                        AnimatedSwitcher(
                          duration: const Duration(milliseconds: 400),
                          child: Text(
                            _message!,
                            key: ValueKey(_message),
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 16,
                              color: _message!.contains("✅")
                                  ? Colors.green
                                  : Colors.red,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),

                      const Spacer(),

                      const Divider(thickness: 1.2),
                      const SizedBox(height: 12),
                      const Text(
                        "لشراء كود التفعيل بسعر 500 ريال تواصل معنا",
                        style: TextStyle(fontSize: 15),
                      ),
                      const SizedBox(height: 10),
                      GestureDetector(
                        onTap: () {
                          // لاحقًا يمكنك تفعيل واتساب هنا
                        },
                        child:  _buildButton(
                          icon: Icons.chat,
                          text: 'اضغط لمراسلتي عبر واتساب',
                          color1: Colors.green.shade600,
                          color2: Colors.green.shade400,
                          onTap: _launchWhatsApp,
                        ),
                      ),
                      const SizedBox(height: 30),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }


  Widget _buildButton({
    required IconData icon,
    required String text,
    required Color color1,
    required Color color2,
    required Function() onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: [color1, color2],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(14),
          boxShadow: [
            BoxShadow(
              color: color1.withOpacity(0.4),
              blurRadius: 10,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: Colors.white, size: 24),
            const SizedBox(width: 10),
            Text(
              text,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontFamily: "Cairo",
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }

}
