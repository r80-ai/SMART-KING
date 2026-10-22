# SMART KING

All PDF Tools in One App: Merge, Split, Compress, Convert, Edit, Scanner, AI Tools, and File Manager.

## Features

- **PDF Tools**: Merge, Split, Compress, Convert, Sign, Protect, Unlock, Reorder, Watermark, and Page Numbering.
- **Document Scanner & OCR**: High-quality document scanning with perspective correction, filters, and text recognition.
- **Photo Editor**: Advanced on-device photo editing with AI background removal powered by BiRefNet Lite, color/gradient/image backgrounds, manual mask brush refinement, transformations, and filters.
- **Digital Signatures**: Sign PDF documents with cryptographic signature support.
- **100% Offline & Private**: On-device processing ensures your documents and photos never leave your device.

## Architecture

- **UI**: Kotlin & Jetpack Compose with Material Design 3
- **AI Background Removal**: ONNX Runtime with BiRefNet Lite (`birefnet_lite_fp16.onnx`)
- **PDF Engine**: Android PdfDocument & PDFBox-Android
- **Persistence**: Room Database for offline file history and tools
