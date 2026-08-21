import urllib.request
import os
from PIL import Image, ImageDraw, ImageFont
import arabic_reshaper
from bidi.algorithm import get_display

def download_font():
    font_url = "https://github.com/google/fonts/raw/main/ofl/amiri/Amiri-Bold.ttf"
    font_path = "Amiri-Bold.ttf"
    if not os.path.exists(font_path):
        urllib.request.urlretrieve(font_url, font_path)
    return font_path

def process_image(img_path, out_path):
    img = Image.open(img_path).convert("RGBA")
    draw = ImageDraw.Draw(img)

    # Image center
    w, h = img.size
    cx, cy = w // 2, h // 2
    r = int(w * 0.17) # Radius for the inner circle, about 17% of width (174px for 1024)

    # Sample colors from the image
    # We want a gold color for the background and charcoal for the text
    gold_bg = (229, 169, 60, 255) # WarmAccentGold (#E5A93C)
    charcoal = (26, 18, 11, 255) # WarmTextPrimary (#1A120B)

    # Draw a gold circle to cover the center
    # Add a charcoal border
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=gold_bg, outline=charcoal, width=8)

    # Inner decorative border
    r2 = r - 15
    draw.ellipse((cx - r2, cy - r2, cx + r2, cy + r2), outline=charcoal, width=3)

    font_path = download_font()
    
    # Try different font sizes until it fits
    text = "القرآن\nالكريم"
    reshaped_text = arabic_reshaper.reshape(text)
    bidi_text = get_display(reshaped_text)

    font_size = int(w * 0.08)
    font = ImageFont.truetype(font_path, font_size)
    
    # Calculate text bounding box
    bbox = draw.multiline_textbbox((0, 0), bidi_text, font=font, align="center")
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    
    tx = cx - tw / 2
    ty = cy - th / 2 - 15 # slight adjustment up

    # Draw shadow
    draw.multiline_text((tx+2, ty+2), bidi_text, font=font, fill=(0,0,0,100), align="center")
    # Draw text
    draw.multiline_text((tx, ty), bidi_text, font=font, fill=charcoal, align="center")

    img = img.convert("RGB")
    img.save(out_path, quality=95)
    print("Saved to", out_path)

if __name__ == "__main__":
    import sys
    img_in = r"C:\Users\Kt\.gemini\antigravity-ide\brain\4ae35159-b4a7-477b-8ec7-28ce2bc20ac3\quran_app_icon_shiny_beige_flipped.jpg"
    img_out = r"C:\Users\Kt\.gemini\antigravity-ide\brain\4ae35159-b4a7-477b-8ec7-28ce2bc20ac3\quran_app_icon_shiny_beige_with_text.jpg"
    process_image(img_in, img_out)
