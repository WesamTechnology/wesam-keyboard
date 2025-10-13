import 'package:shared_preferences/shared_preferences.dart';

class ActivationService {
  static const _keyActivated = 'isActivated';

  /// حفظ حالة التفعيل بعد إدخال الكود بنجاح
  static Future<void> setActivated() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_keyActivated, true);
  }

  /// قراءة الحالة عند التشغيل
  static Future<bool> isActivated() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_keyActivated) ?? false;
  }

  /// لإعادة التطبيق إلى وضع غير مفعل (اختياري)
  static Future<void> resetActivation() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyActivated);
  }
}
