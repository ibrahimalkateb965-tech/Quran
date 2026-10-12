import { describe, test } from '@e2e-dev/mobile';
import { expect } from 'e2e';

// Blind-first flows on the Quran player. Every locator targets what TalkBack
// reads (contentDescription / merged text), never coordinates or visuals.
// The group is serial: one device, and each step continues from the previous screen.

const PAGE_BUTTON = /^عرضُ صفحةِ الآيةِ كاملةً في المصحف، صفحة \d+$/;
const SEARCH_FIELD = /^حقل البحث برقم الآية في صفحة \d+$/;
const AYAH_CARD = /^الآية \d+ من سورة \S.*\. \S/;
const NO_MATCH = /^لا توجد آية برقم «999» في هذه الصفحة$/;

describe('quran player — TalkBack semantics', { serial: true }, () => {
  test('player screen exposes labelled controls', async ({ app, screen }) => {
    await app.open();

    await expect(screen.getByLabel('اختيارُ السورة')).toBeVisible({ timeout: 15_000 });
    await expect(screen.getByLabel('اختيارُ القارئ')).toBeVisible();
    await expect(screen.getByLabel(/^الاستماع المتواصل\. حالياً (معطل|مفعل)$/)).toBeVisible();
    await expect(screen.getByRole('button', PAGE_BUTTON)).toBeVisible();
  });

  test('page sheet opens in text mode with labelled tabs and ayah cards', async ({ screen }) => {
    await screen.getByRole('button', PAGE_BUTTON).tap();

    await expect(screen.getByLabel('عرض نص الآيات العثماني')).toBeVisible({ timeout: 10_000 });
    await expect(screen.getByLabel('عرض صورة صفحة مصحف المدينة المصورة')).toBeVisible();
    // Text mode is the default: the ayah-number search field is present without switching tabs.
    await expect(screen.getByLabel(SEARCH_FIELD)).toBeVisible();
    // Each ayah card announces number + surah + full Uthmanic text in one TalkBack stop.
    await expect(screen.getByLabel(AYAH_CARD).first()).toBeVisible();
  });

  test('ayah search: no match shows empty state, clear button restores the list', async ({ screen }) => {
    await screen.getByLabel(SEARCH_FIELD).fill('999');

    await expect(screen.getByText(NO_MATCH)).toBeVisible();
    await expect(screen.getByLabel(AYAH_CARD)).toHaveCount(0);

    // The in-body clear button must be a real, tappable target (not collapsed to 0 px).
    const clear = screen.getByText('مسح البحث', { visible: true });
    await expect(clear).toBeVisible();
    await clear.tap();

    await expect(screen.getByText(NO_MATCH)).toBeHidden();
    await expect(screen.getByLabel(AYAH_CARD).first()).toBeVisible();
  });

  test('page jump dialog: open, enter page number, jump to target page', async ({ screen }) => {
    // Tap the page number badge on the header
    const pageBadge = screen.getByLabel(/^صفحة \d+، الجزء \d+\. انقر مرتين للبحث برقم الصفحة والانتقالِ إليها\.$/);
    await expect(pageBadge).toBeVisible();
    await pageBadge.tap();

    // Dialog appears with accessible input field
    const jumpInput = screen.getByLabel('حقل إدخال رقم الصفحة المطلوب الانتقال إليها');
    await expect(jumpInput).toBeVisible({ timeout: 5_000 });

    // Enter target page number
    await jumpInput.fill('2');

    // Tap confirm button
    const confirmButton = screen.getByText('انتقال');
    await expect(confirmButton).toBeVisible();
    await confirmButton.tap();

    // Verify sheet updated to page 2
    await expect(screen.getByLabel(/^صفحة 2، الجزء \d+\. انقر مرتين للبحث برقم الصفحة والانتقالِ إليها\.$/)).toBeVisible({ timeout: 5_000 });
  });

  test('back closes the sheet and returns focus to the player', async ({ screen, device }) => {
    // The first back may only dismiss the IME; a second one then closes the sheet.
    await device.back();
    if (await screen.getByLabel(SEARCH_FIELD).isVisible()) await device.back();

    await expect(screen.getByRole('button', PAGE_BUTTON)).toBeVisible({ timeout: 10_000 });
    await expect(screen.getByLabel(SEARCH_FIELD)).toBeHidden();
  });
});
