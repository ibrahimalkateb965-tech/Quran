import os
import re
import sys
import codecs

# إصلاح ترميز الكونسول في ويندوز
if sys.stdout.encoding.lower() != 'utf-8':
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.detach())

AGENTS_DIR = os.path.dirname(os.path.abspath(__file__))
MEMORY_STORE_PATH = os.path.join(AGENTS_DIR, "MEMORY_STORE.md")
CONTEXT_INJECTION_PATH = os.path.join(AGENTS_DIR, "ACTIVE_CONTEXT_INJECTION.md")

def extract_active_lessons(filepath):
    if not os.path.exists(filepath):
        return []
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    yaml_matches = re.findall(r'```yaml(.*?)```', content, re.DOTALL)
    if not yaml_matches:
        return []
    
    active_entries = []
    
    for yaml_content in yaml_matches:
        blocks = yaml_content.split('- id:')
        for block in blocks[1:]:
            block_lower = block.lower()
            if 'status: active' in block_lower and ('type: lesson' in block_lower or 'type: bug-fix' in block_lower or 'type: decision' in block_lower):
                # استخراج السياق والمحتوى
                context_match = re.search(r'context:\s*"(.*?)"', block, re.DOTALL)
                content_match = re.search(r'content:\s*"(.*?)"', block, re.DOTALL)
                
                if context_match and content_match:
                    active_entries.append({
                        'context': context_match.group(1).replace('\n', ' '),
                        'content': content_match.group(1).replace('\n', ' ')
                    })
                    
    return active_entries

def main():
    print("🔍 جاري فحص الذاكرة وتجهيز سياق الحقن الإجباري...")
    
    entries = extract_active_lessons(MEMORY_STORE_PATH)
    
    if not entries:
        print("⚠️ لم يتم العثور على دروس نشطة للحقن.")
        return

    # كتابة ملف ACTIVE_CONTEXT_INJECTION.md
    with open(CONTEXT_INJECTION_PATH, 'w', encoding='utf-8') as f:
        f.write("<div dir=\"rtl\">\n\n")
        f.write("# 🛡️ سياق الحقن الإجباري (Mandatory Context Injection)\n\n")
        f.write("> **يجب قراءة هذه القيود والالتزام التام بها قبل كتابة أي سطر كود.**\n\n")
        
        for i, entry in enumerate(entries, 1):
            f.write(f"### القيد {i}: {entry['context']}\n")
            f.write(f"**القرار/الدرس:** {entry['content']}\n\n")
            
        f.write("</div>\n")
        
    print(f"✅ تم حقن {len(entries)} دروس/قرارات نشطة في .agents/ACTIVE_CONTEXT_INJECTION.md")
    print("\n> [!WARNING] Mandatory Constraints for Agents:")
    
    # طباعة ملخص سريع للشات لإجباره في نافذة السياق
    # نعرض آخر 5 فقط لتجنب إغراق المحادثة
    for entry in entries[-5:]:
        print(f"🚨 {entry['context']}")
        print(f"   => {entry['content'][:150]}...\n")
        
    print("💡 تنبيه للوكيل (Agent Alert): يمنع توليد الكود قبل مراجعة ACTIVE_CONTEXT_INJECTION.md والتصريح بذلك.")

if __name__ == "__main__":
    main()
