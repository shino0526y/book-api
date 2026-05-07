# book-api

book-api は、著者と書籍を管理するための Spring Boot 4 ベースの Web API です。Kotlin で実装されており、PostgreSQL を永続化層、Flyway をマイグレーション、jOOQ を SQL アクセスに利用しています。

Swagger UI と OpenAPI を有効化しているため、起動後すぐに API をブラウザから確認できます。

## 主な機能

- 著者の登録と更新
- 書籍の登録と更新
- 著者ごとの書籍一覧取得
- 入力バリデーションと業務ルールの検証
- OpenAPI JSON と Swagger UI の公開

## 技術スタック

- Kotlin 2.3.21
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Validation
- PostgreSQL
- Flyway
- jOOQ
- springdoc-openapi
- Gradle Wrapper 9.4.1

## 前提条件

- Java 25
- PostgreSQL

このプロジェクトは Gradle Toolchain を使って Java 25 を前提にビルドされます。

## セットアップ

### 1. Dev Container を使う場合

このリポジトリには Dev Container 設定が含まれており、起動時に以下が自動で設定されます。

- PostgreSQL サービスの起動
- ロール `book_api` の作成
- データベース `book_api` の作成
- アプリケーション用の環境変数設定

既定の接続情報は次のとおりです。

```env
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
POSTGRES_DB=book_api
POSTGRES_USER=book_api
POSTGRES_PASSWORD=book_api
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/book_api
SPRING_DATASOURCE_USERNAME=book_api
SPRING_DATASOURCE_PASSWORD=book_api
```

### 2. ローカル環境で動かす場合

PostgreSQL 側でロールとデータベースを用意したうえで、次の環境変数を設定してください。

```env
export POSTGRES_HOST=127.0.0.1
export POSTGRES_PORT=5432
export POSTGRES_DB=book_api
export POSTGRES_USER=book_api
export POSTGRES_PASSWORD=book_api

export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/book_api
export SPRING_DATASOURCE_USERNAME=book_api
export SPRING_DATASOURCE_PASSWORD=book_api
```

`POSTGRES_*` は Flyway と jOOQ の接続先として使われます。`SPRING_DATASOURCE_*` はアプリケーション本体のデータソース設定に使われます。

## 起動方法

```bash
./gradlew bootRun
```

起動時に Flyway マイグレーションが適用され、必要な jOOQ コード生成もビルド時に実行されます。

起動後のアクセス先:

- API ベース URL: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## テスト

```bash
./gradlew test
```

## ビルド

```bash
./gradlew build
```

WAR を生成したい場合は次を利用できます。

```bash
./gradlew bootWar
```

## API 一覧

| メソッド | パス | 説明 |
| --- | --- | --- |
| POST | `/api/authors` | 著者を登録する |
| PUT | `/api/authors/{authorId}` | 著者を更新する |
| GET | `/api/authors/{authorId}/books` | 著者に紐づく書籍一覧を取得する |
| POST | `/api/books` | 書籍を登録する |
| PUT | `/api/books/{bookId}` | 書籍を更新する |

## 主なリクエスト仕様

### 著者

リクエスト項目:

- `name`: 必須、空文字不可
- `birthDate`: 必須、現在日以前

作成例:

```bash
curl -X POST http://localhost:8080/api/authors \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Ursula K. Le Guin",
    "birthDate": "1929-10-21"
  }'
```

### 書籍

リクエスト項目:

- `title`: 必須、空文字不可
- `price`: 必須、`0.00` 以上
- `authorIds`: 必須、1 件以上、正の数のみ、存在する著者 ID のみ指定可
- `publicationStatus`: 必須、`UNPUBLISHED` または `PUBLISHED`

作成例:

```bash
curl -X POST http://localhost:8080/api/books \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Good Omens",
    "price": 1800.00,
    "authorIds": [1, 2],
    "publicationStatus": "UNPUBLISHED"
  }'
```

著者別の書籍一覧取得例:

```bash
curl http://localhost:8080/api/authors/1/books
```

## 業務ルール

- 書籍には少なくとも 1 人の著者が必要です
- 存在しない著者 ID は書籍に紐づけできません
- 一度 `PUBLISHED` になった書籍は `UNPUBLISHED` に戻せません

最後のルールはアプリケーション層に加えて、PostgreSQL のトリガーでも保護されています。

## エラーレスポンス

エラー時は次の形式の JSON を返します。

```json
{
  "message": "validation failed",
  "details": [
    "fieldName: error message"
  ]
}
```

代表的なステータス:

- `400 Bad Request`: 入力不正、存在しない著者 ID、JSON 形式不正など
- `404 Not Found`: 対象の著者または書籍が存在しない
- `409 Conflict`: 公開済み書籍を未公開に戻そうとした場合

## データベース概要

主なテーブル:

- `authors`
- `books`
- `book_authors`

Flyway の初期マイグレーションで以下が定義されています。

- テーブル作成
- 制約とインデックス
- `updated_at` 更新用トリガー
- 書籍の公開状態逆戻し防止トリガー
- 書籍に著者が 1 件以上必要であることを保証する制約トリガー

## ディレクトリ構成

```text
src/main/kotlin/io/github/shino0526y/book_api/
  api/         DTO と enum
  controller/  REST API エンドポイント
  service/     業務ロジック
  exception/   例外と API エラーハンドリング

src/main/resources/
  application.yaml
  db/migration/
```

## 補足

- Swagger UI は `/swagger-ui.html` で公開され、実体は `/swagger-ui/index.html` にリダイレクトされます
- OpenAPI JSON は `/v3/api-docs` で取得できます
- jOOQ の生成コードはビルドディレクトリ配下に出力されます
