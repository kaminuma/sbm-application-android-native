# SBM Application - Android Native

[![Android](https://img.shields.io/badge/platform-Android-green)](https://github.com/kaminuma/sbm-application_UI/tree/main/android-native)
[![API Level](https://img.shields.io/badge/API-26%2B-brightgreen)](https://developer.android.com/guide/topics/manifest/uses-sdk-element)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple)](https://kotlinlang.org/)

SBMアプリケーションのAndroid ネイティブ版です。Kotlin + Jetpack Compose + Material 3 デザインを採用したモダンな生活記録・スケジュール管理アプリです。

## ✨ 機能概要

- **ユーザー認証**: JWT認証によるセキュアなログイン・新規登録
- **アクティビティ管理**: 生活記録の作成・表示・削除（カテゴリ・時間・内容）
- **カレンダー機能**: 月間表示・週間表示・日付選択・アクティビティ表示
- **ムード記録**: 5段階評価（😢〜😄）による感情記録・履歴表示
- **データ分析**: アクティビティカテゴリ分析・ムード傾向チャート表示
- **週表示カレンダー**: 時間軸による週間スケジュール・ムードアイコン統合
- **✨ AI ライフガイド**: カスタマイズ可能なAI分析（期間・焦点・詳細度・口調を選択可能）

## 🛠️ 技術スタック

### コア技術
- **Kotlin 1.9.22**: プログラミング言語
- **Jetpack Compose**: UI フレームワーク
- **Material 3**: デザインシステム
- **Android SDK 26-34**: プラットフォーム対応

### 主要ライブラリ
- **Dagger Hilt 2.48**: 依存性注入
- **Retrofit 2.9.0 + OkHttp 4.12.0**: HTTP通信
- **Gson 2.10.1**: JSON シリアライゼーション
- **Navigation Compose 2.7.6**: 画面遷移
- **ComposeCalendar 1.1.0**: カレンダー機能
- **EncryptedSharedPreferences**: セキュアなデータ保存
- **Vico 1.13.1**: チャート表示

## 🏗️ アーキテクチャ

### 設計パターン
- **Clean Architecture**: ドメイン・データ・プレゼンテーション層の分離
- **MVVM**: ViewModelsによるUI状態管理
- **Repository Pattern**: データアクセスの抽象化
- **Dependency Injection**: Dagger Hiltによる依存性管理

### プロジェクト構成
```
app/src/main/java/com/sbm/application/
├── data/                           # データ層
│   ├── network/                    # ネットワーク関連
│   ├── remote/                     # API通信
│   └── repository/                 # リポジトリ実装
├── domain/                         # ドメイン層
│   ├── model/                      # ドメインモデル
│   └── repository/                 # リポジトリインターフェース
├── presentation/                   # プレゼンテーション層
│   ├── components/                 # 再利用可能なUIコンポーネント
│   ├── screen/                     # 画面コンポーネント
│   ├── viewmodel/                  # ViewModels
│   └── theme/                      # テーマ・スタイル定義
└── di/                            # 依存性注入設定
```

## 🚀 セットアップ

### 必要条件
- Android Studio Arctic Fox 以上
- Android SDK 26-34
- Java 11 以上

### インストール手順

1. **リポジトリクローン**
```bash
git clone https://github.com/kaminuma/sbm-application_UI.git
cd sbm-application_UI/android-native
```

2. **Android Studio でプロジェクトを開く**
```
File -> Open -> android-native フォルダを選択
```

3. **環境設定**
プロジェクトルートに `local.properties` ファイルを作成（既存の場合は追記）：

```properties
# Android SDK path
sdk.dir=/Users/yourname/Library/Android/sdk

# SBM API Configuration
API_BASE_URL=https://api.sbm-app.com/api/v1/

# AI Configuration (オプション - 開発用)
GEMINI_API_KEY=your_gemini_api_key_here
```

**AI機能について**: 
- 本番環境では API 経由で AI 機能を提供予定
- 開発・テスト用途でのみ直接 Gemini API を使用
- [Google AI Studio](https://makersuite.google.com/app/apikey) で API Key を取得可能

**注意**: `local.properties` はgitignoreに含まれており、リポジトリにはコミットされません。

### ビルドコマンド

```bash
# デバッグビルド
./gradlew assembleDebug

# リリースビルド
./gradlew assembleRelease

# テスト実行
./gradlew test

# アプリインストール
./gradlew installDebug
```

## ✨ AI 分析機能

### AI カスタマイズオプション
- **分析期間**: カスタム期間、直近1週間/1ヶ月/3ヶ月
- **分析の焦点**: 気分重視、活動重視、バランス、ウェルネス重視
<!-- TODO: 将来的に比較オプション（前回同期間、先月、去年同期）を追加予定 -->
- **詳細レベル**: 簡潔、標準、詳細
- **応答スタイル**: 親しみやすい、専門的、励まし重視、カジュアル

### AI 機能の特徴
- **ユーザーフレンドリー**: 技術的な設定は不要、直感的なカスタマイズ
- **パーソナライズ**: 4つのカテゴリで自分好みの分析スタイルを選択
- **動的プロンプト**: 設定に応じてAIの応答内容が自動調整
- **将来対応**: API 経由での AI 提供に向けた設計

### 使い方
1. 分析画面の「✨ AI ライフガイド 🤖」の歯車アイコンをタップ
2. 4つのカテゴリで好みの設定を選択（自動保存）
3. 分析画面でAI分析ボタンを押してカスタマイズされた洞察を取得

## 🎨 UI/UX 特徴

### Material 3 Design
- **Dynamic Color**: システムテーマ連動の色彩設計
- **Adaptive Layout**: 画面サイズに応じた最適化
- **Motion & Animation**: 直感的な画面遷移

### モバイル最適化
- **タッチターゲット**: 56dp最小サイズ保証
- **フォントサイズ**: アクセシビリティ対応
- **カテゴリ選択**: 絵文字付きビジュアルグリッド
- **ムード選択**: 感情絵文字による直感的操作

## 📅 週表示カレンダー

### 機能詳細
- **時間軸表示**: 6:00-23:00の時間スロットによる週間スケジュール
- **ムード統合**: 各日のムードアイコン（😢😔😐😊😄）を日付ヘッダー上に表示
- **直感的操作**: 
  - ムードセル: タップで追加/編集モーダル
  - 時間スロット: タップでアクティビティ追加
  - 週ナビゲーション: 前週/次週の矢印ボタン
- **表示切替**: 月表示/週表示のタブ切り替え

## 🌐 ネットワーク機能

### エラーハンドリング
- **詳細エラー分類**: 接続エラー・タイムアウト・サーバーエラー・認証エラーを個別処理
- **自動リトライ**: 指数バックオフによる最大3回のリトライ処理
- **ユーザーフレンドリー表示**: エラー種別に応じたアイコンとメッセージ
- **リアルタイム監視**: NetworkMonitorによるオフライン状態即座検出

### リトライ機能
```kotlin
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T
```

## 📊 データ可視化

### チャート機能
- **カテゴリ分析**: アクティビティ時間の棒グラフ
- **ムード推移**: 過去30日間の感情変化線グラフ
- **統計概要**: 総アクティビティ数・平均ムード・最活用カテゴリ

## 🧪 テスト

### テスト構成
```bash
# Unit Tests
./gradlew test

# UI Tests  
./gradlew connectedAndroidTest

# Test Coverage
./gradlew jacocoTestReport
```

### テストフレームワーク
- **JUnit 4**: 単体テスト
- **Mockito**: モック作成
- **Espresso**: UI テスト（予定）

## 📈 パフォーマンス

### 最適化項目
- **Compose Recomposition**: 最小限の再描画
- **Image Loading**: 効率的な画像キャッシュ
- **Network**: OkHttpによる接続プール
- **Memory**: ViewModel ライフサイクル管理


## 🐛 トラブルシューティング

### よくある問題

1. **ビルドエラー**: Gradle Syncを実行してください
2. **API接続エラー**: build.gradle.ktsのAPI_BASE_URLを確認
3. **認証エラー**: アプリを再起動してトークンをリフレッシュ

### ログ確認
```bash
# アプリログ確認
adb logcat | grep "SBM"
```

## 📋 制限事項

- **オフライン機能**: 現在はオンライン専用（Room データベース統合は今後予定）
- **プッシュ通知**: 未実装
- **多言語対応**: 現在日本語のみ

## 🔮 今後の予定

### 短期計画
- AI 機能のAPI経由移行（プロキシAPI実装）
- AI 設定のサーバー側永続化
- Room データベース統合（オフライン対応）
- プルトゥリフレッシュ機能

### 長期計画
- AI 分析履歴機能
- 複数AI プロバイダー対応（Gemini, Claude, GPT）
- ウィジェット機能
- プッシュ通知機能

## 🤝 コントリビューション

### 開発ガイドライン
1. Clean Architecture パターンに従う
2. Material 3 デザインガイドラインを準拠
3. Unit テストを含める
4. Kotlin コーディング規約に従う

### プルリクエスト
1. Feature ブランチを作成
2. 変更をコミット
3. テストを実行
4. プルリクエストを作成

## 📄 ライセンス

このプロジェクトは **Apache License 2.0** の下でライセンスされています。

## 📧 サポート

バグ報告や機能リクエストは、[Issues](https://github.com/kaminuma/sbm-application_UI/issues) をご利用ください。