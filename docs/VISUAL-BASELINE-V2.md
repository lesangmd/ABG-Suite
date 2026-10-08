# MEDIPHARM Khí Máu — Brand + Visual Baseline v2

Status: CANONICAL DESIGN BASELINE
Scope: WebApp shell/responsive first. Android packaging is explicitly deferred until WebApp visual acceptance.
Parent functional/content baseline: WebApp v6.11.4. Business logic and clinical content are preserved.

## 1. Brand
Product name: MEDIPHARM Khí Máu.
Logo: redesigned O2/CO2 blood-gas mark, simplified for small sizes. Light app icon = white rounded tile; dark app icon = navy rounded tile.
Brand language: simple, precise, clinical. Avoid audit/developer terminology in user-facing UI.

## 2. Core color tokens
--brand-primary: #2563EB;
--brand-secondary: #0EA5E9;
--brand-accent: #EF4444;
--brand-success: #10B981;
--brand-warning: #F59E0B;
--ink: #0F2942;
--muted: #64748B;
--surface: #FFFFFF;
--background: #F8FAFC;
--border: #DCE7EF;

Module accents:
Analysis = blue; Clinical cases = coral; Practice = teal/green; Dictionary = violet.
Gradients are reserved for hero, brand graphics and primary CTA. Ordinary cards remain neutral.

## 3. Information architecture
Persistent primary navigation contains exactly four destinations:
1. Trang chủ
2. Phân tích
3. Ca lâm sàng
4. Học tập

Remove “Thêm” from the Dock.
Menu contains secondary actions: Ôn luyện, Từ điển, Chế độ tối, Kiểm tra cập nhật, Giới thiệu ứng dụng, Hướng dẫn sử dụng, Liên hệ hỗ trợ, Lưu ý an toàn, Đăng xuất.
Hỏi đáp is merged into Ôn luyện.
Dictionary definitions render inline below the term; no definition modal.

## 4. Responsive shell
Breakpoints are content-driven, with reference ranges:
- compact/mobile: <= 767px
- medium/tablet/foldable: 768–1199px
- expanded/desktop: >= 1200px

Compact: true one-column layout; no compressed desktop grids.
Medium: adaptive 2-column modules where content permits.
Expanded: desktop composition uses horizontal space; never scale up the mobile shell.
Foldable: support single-pane and dual-pane modes according to available width; menu may occupy a secondary pane.

All layouts must respect safe-area insets. The primary Dock is outside the scroll container and remains visible. Scroll content receives bottom padding equal to Dock + safe-area height.

## 5. Home
Header: compact brand lockup + adaptive search + account + menu.
Hero: title, concise clinical description, primary CTA, restrained ABG/lung visual.
Primary modules: Analysis, Clinical cases, Practice, Dictionary.
Featured content follows the primary modules; it must not compete visually with the clinical CTA.

## 6. Analysis workspace
Tabs/steps: Nhập dữ liệu → Kết quả → Diễn giải → Sơ đồ → Khuyến cáo.
Forms use minimum touch target 44px, clear units, consistent label/input alignment, no horizontal overflow.
On mobile, form fields stack or use deliberate two-column pairs only when both remain readable.

## 7. Clinical cases
Replace long unstructured page with: Tổng quan · Diễn biến · Khí máu · Câu hỏi · Bàn luận.
Case demographics are compact metadata, not oversized prose.
Clinical sections use semantic cards and readable measure; no modal for the whole case.

## 8. Learning
Học tập is the primary destination; Ôn luyện is a module within it.
Question screen: specialty/topic selector, progress, question, options, explicit reveal/explanation action.
No full-screen nested popup for routine learning content.

## 9. Login
Same brand system as app shell. Responsive centered card on light clinical background with restrained red/blue wave graphics.
No generic Android-dialog visual language in the WebApp.
Registration remains a clear secondary action.

## 10. Acceptance gates
Must pass at representative widths: 320, 360, 390, 412, 480, 600, 768, 820, 1024, 1200, 1440, 1920 px.
Must also pass landscape phone and foldable postures.
No horizontal page overflow; no clipped title; no Dock overlap; no duplicated navigation; no modal larger than usable viewport.
Light and dark modes must retain WCAG-oriented contrast and the same information hierarchy.

## Release order
Wave 1: design tokens + logo/brand assets + shell/header/search/menu/dock.
Wave 2: Home.
Wave 3: Analysis.
Wave 4: Clinical cases.
Wave 5: Learning + Practice + Dictionary.
Wave 6: Login + dark mode + foldable/desktop regression.
Only after WebApp acceptance: package Android using the accepted WebApp bundle.
