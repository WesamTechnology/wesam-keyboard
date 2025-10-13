import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class CoreTeam extends StatelessWidget {
  const CoreTeam({super.key});
  static const screenRoute = '/team';

  Future<void> _launchPhone() async {
    final Uri url = Uri.parse('tel:+967775904988');
    if (await canLaunchUrl(url)) {
      await launchUrl(url);
    }
  }

  Future<void> _launchWhatsApp() async {
    final Uri whatsapp = Uri.parse('https://wa.me/967775904988');
    if (await canLaunchUrl(whatsapp)) {
      await launchUrl(whatsapp, mode: LaunchMode.externalApplication);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        title: const Text(
          'من أنا',
          style: TextStyle(
            fontFamily: 'Cairo',
            fontWeight: FontWeight.bold,
          ),
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [
              Color(0xFF0D47A1), // أزرق غامق
              Color(0xFF1976D2), // أزرق متوسط
              Color(0xFF64B5F6), // أزرق سماوي فاتح
            ],
            begin: Alignment.topRight,
            end: Alignment.bottomLeft,
          ),
        ),
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 60),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // صورة شخصية مع ظل جميل
              Container(
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.25),
                      blurRadius: 15,
                      offset: const Offset(0, 6),
                    ),
                  ],
                ),
                child: const CircleAvatar(
                  radius: 75,
                  backgroundImage: AssetImage('assets/images/profile.jpg'),
                ),
              ),
              const SizedBox(height: 24),

              const Text(
                'وسام الجنيد',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 30,
                  fontWeight: FontWeight.bold,
                  fontFamily: 'Cairo',
                  shadows: [
                    Shadow(color: Colors.black38, blurRadius: 10),
                  ],
                ),
              ),
              const SizedBox(height: 8),

              const Text(
                'مطور تطبيقات Flutter وتقنية المعلومات',
                style: TextStyle(
                  fontSize: 18,
                  color: Colors.white70,
                  fontFamily: "Cairo",
                ),
                textAlign: TextAlign.center,
              ),

              const SizedBox(height: 28),

              // بطاقة شفافة جميلة
              Container(
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: Colors.white.withOpacity(0.3)),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.2),
                      blurRadius: 15,
                      offset: const Offset(0, 8),
                    ),
                  ],
                ),
                padding: const EdgeInsets.all(20),
                child: const Text(
                  'أنا وسام الجنيد، مطوّر تطبيقات ومبرمج شغوف في Flutter. أعمل على إنشاء تطبيقات حديثة بأفكار مبتكرة وتصاميم جذابة. هدفي هو جعل التقنية سهلة وممتعة للجميع.',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 17,
                    height: 1.6,
                    fontFamily: "Cairo",
                  ),
                ),
              ),

              const SizedBox(height: 40),

              // أزرار التواصل
              _buildButton(
                icon: Icons.phone,
                text: 'اتصال: +967775904988',
                color1: const Color(0xFF1976D2),
                color2: const Color(0xFF42A5F5),
                onTap: _launchPhone,
              ),
              const SizedBox(height: 18),
              _buildButton(
                icon: Icons.chat,
                text: 'راسلني عبر واتساب',
                color1: Colors.green.shade600,
                color2: Colors.green.shade400,
                onTap: _launchWhatsApp,
              ),
            ],
          ),
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
