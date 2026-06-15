# Scalactive Document

## 起動方法

リポジトリルートで以下を実行.

```
sbt ~doc/fastLinkJS
```

並行して以下を実行.

```
cd doc
npm install -g yarn
yarn
yarn dev
```

## ビルド方法

```
cd doc
yarn build
```
