import os
import re
import sys
import codecs

# إصلاح مشكلة الترميز في سطر الأوامر (Windows Console UTF-8 Fix)
if sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.detach())

# إعداد المسارات
LOCAL_MEMORY_PATH = r"f:\AI PROJECTS\Blind App\.agents\MEMORY_STORE.md"
GLOBAL_REFERENCES_DIR = r"C:\Users\Kt\.gemini\config\references"
GLOBAL_MEMORY_PATH = os.path.join(GLOBAL_REFERENCES_DIR, "GLOBAL_MEMORY_STORE.md")

def extract_global_memories(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # البحث عن جميع كتل YAML
    yaml_matches = re.findall(r'```yaml(.*?)```', content, re.DOTALL)
    if not yaml_matches:
        return []
    
    global_entries = []
    
    for yaml_content in yaml_matches:
        # فصل الذكريات بناءً على الـ ID
        blocks = yaml_content.split('- id:')
        
        for block in blocks[1:]:
            # التحقق من وجود وسم 'global'
            if 'global' in block.lower():  
                entry = "- id:" + block
                global_entries.append(entry.strip())
            
    return global_entries

def main():
    print("="*50)
    print("🔄 بدء فحص الذاكرة المحلية بحثاً عن دروس عالمية...")
    print("="*50)
    
    if not os.path.exists(LOCAL_MEMORY_PATH):
        print("❌ ملف الذاكرة المحلية غير موجود!")
        return
        
    entries = extract_global_memories(LOCAL_MEMORY_PATH)
    
    if not entries:
        print("⚠️ لم يتم العثور على ذكريات مصنفة كعالمية.")
        print("💡 تلميح: قم بإضافة وسم 'global' إلى مصفوفة الـ tags لأي درس في MEMORY_STORE.md ليتم تصديره.")
        return
        
    print(f"✅ تم العثور على {len(entries)} ذكريات تحمل وسم 'global'.")
    
    # إنشاء المجلد العالمي إذا لم يكن موجوداً
    os.makedirs(GLOBAL_REFERENCES_DIR, exist_ok=True)
    
    existing_content = ""
    if os.path.exists(GLOBAL_MEMORY_PATH):
        with open(GLOBAL_MEMORY_PATH, 'r', encoding='utf-8') as f:
            existing_content = f.read()
            
    new_entries_count = 0
    
    # استخراج الذكريات الجديدة فقط لمنع التكرار
    entries_to_add = []
    for entry in entries:
        id_match = re.search(r'(MEM|BUG|ADR)-\d{4}-\d{2}-\d{2}-\d+', entry)
        entry_id = id_match.group(0) if id_match else None
        
        if entry_id and entry_id not in existing_content:
            entries_to_add.append(entry)
            new_entries_count += 1
            
    if new_entries_count == 0:
        print("ℹ️ جميع الذكريات العالمية موجودة بالفعل في المجلد العام (لا يوجد جديد لإضافته).")
        return

    # عملية الإضافة
    is_new_file = not existing_content.strip()
    
    with open(GLOBAL_MEMORY_PATH, 'a', encoding='utf-8') as f:
        if is_new_file:
            f.write("<div dir=\"rtl\">\n\n# 🧠 الذاكرة العالمية المجمعة (Global Memory Store)\n\n")
            f.write("> **يحتوي هذا الملف على الذكريات والدروس المستفادة التي تم تصديرها أوتوماتيكياً من مختلف المشاريع.**\n\n")
            f.write("```yaml\n")
            
        for entry in entries_to_add:
            f.write(entry + "\n\n")
            
        if is_new_file:
            f.write("```\n\n</div>\n")
            
    print(f"🚀 تم تصدير {new_entries_count} ذكريات جديدة بنجاح إلى المركز العالمي!")
    print(f"📁 المسار: {GLOBAL_MEMORY_PATH}")
    print("="*50)

if __name__ == "__main__":
    main()
