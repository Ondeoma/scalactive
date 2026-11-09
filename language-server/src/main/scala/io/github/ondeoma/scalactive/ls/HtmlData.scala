package io.github.ondeoma.scalactive.ls

/**
 * HTMLの標準のタグ・属性・イベント(テンプレートの補完とホバーで使います).
 * よく使うものに絞っています.
 */
object HtmlData {

  /** タグ名 → 説明 */
  val tags: List[(String, String)] = List(
    "a" -> "ハイパーリンク", "abbr" -> "略語", "address" -> "連絡先", "article" -> "独立した記事", "aside" -> "補足",
    "audio" -> "音声", "b" -> "太字", "blockquote" -> "引用(ブロック)", "br" -> "改行", "button" -> "ボタン",
    "canvas" -> "描画領域", "caption" -> "表の見出し", "cite" -> "引用元", "code" -> "コード", "col" -> "表の列",
    "colgroup" -> "表の列のグループ", "data" -> "機械可読な値", "datalist" -> "入力候補", "dd" -> "説明リストの説明",
    "del" -> "削除された内容", "details" -> "開閉できる詳細", "dfn" -> "定義語", "dialog" -> "ダイアログ", "div" -> "汎用のブロック",
    "dl" -> "説明リスト", "dt" -> "説明リストの用語", "em" -> "強調", "embed" -> "埋め込み", "fieldset" -> "フォームのグループ",
    "figcaption" -> "図の説明", "figure" -> "図", "footer" -> "フッター", "form" -> "フォーム", "h1" -> "見出し1",
    "h2" -> "見出し2", "h3" -> "見出し3", "h4" -> "見出し4", "h5" -> "見出し5", "h6" -> "見出し6", "header" -> "ヘッダー",
    "hr" -> "区切り線", "i" -> "斜体", "iframe" -> "インラインフレーム", "img" -> "画像", "input" -> "入力欄",
    "ins" -> "追加された内容", "kbd" -> "キーボード入力", "label" -> "ラベル", "legend" -> "fieldsetの見出し", "li" -> "リストの項目",
    "main" -> "主な内容", "mark" -> "ハイライト", "menu" -> "メニュー", "meter" -> "計測値", "nav" -> "ナビゲーション",
    "object" -> "外部リソース", "ol" -> "番号付きリスト", "optgroup" -> "選択肢のグループ", "option" -> "選択肢", "output" -> "計算結果",
    "p" -> "段落", "picture" -> "画像の切り替え", "pre" -> "整形済みテキスト", "progress" -> "進捗", "q" -> "引用(インライン)",
    "s" -> "取り消し線", "samp" -> "出力例", "section" -> "セクション", "select" -> "選択リスト", "slot" -> "スロット(Props の Slot を描画)",
    "small" -> "注記", "source" -> "メディアのソース", "span" -> "汎用のインライン", "strong" -> "重要", "sub" -> "下付き",
    "summary" -> "detailsの見出し", "sup" -> "上付き", "svg" -> "SVG", "table" -> "表", "tbody" -> "表の本体", "td" -> "表のセル",
    "template" -> "テンプレート(ディレクティブ付きなら子だけを描画)", "textarea" -> "複数行の入力欄", "tfoot" -> "表のフッター",
    "th" -> "表の見出しセル", "thead" -> "表のヘッダー", "time" -> "日時", "tr" -> "表の行", "track" -> "字幕", "u" -> "下線",
    "ul" -> "リスト", "var" -> "変数", "video" -> "動画", "wbr" -> "改行してよい位置",
  )

  val tagDescriptions: Map[String, String] = tags.toMap

  /** すべての要素で使える属性 */
  val globalAttributes: List[(String, String)] = List(
    "id" -> "要素のid", "class" -> "クラス", "style" -> "インラインのスタイル", "title" -> "補足の説明(ツールチップ)",
    "hidden" -> "非表示", "tabindex" -> "フォーカスの順番", "lang" -> "言語", "dir" -> "文字の方向", "draggable" -> "ドラッグできるか",
    "contenteditable" -> "編集できるか", "role" -> "ARIAのロール", "aria-label" -> "読み上げ用のラベル", "aria-hidden" -> "読み上げから隠す",
    "aria-describedby" -> "説明の要素のid", "aria-expanded" -> "開いているか", "aria-controls" -> "操作する要素のid",
    "inert" -> "操作できなくする", "popover" -> "ポップオーバー", "translate" -> "翻訳してよいか", "spellcheck" -> "スペルチェック",
    "autofocus" -> "自動でフォーカス", "accesskey" -> "ショートカットキー", "enterkeyhint" -> "Enterキーの表示", "inputmode" -> "仮想キーボードの種類",
  )

  /** 要素ごとの属性 */
  val elementAttributes: Map[String, List[(String, String)]] = Map(
    "a" -> List("href" -> "リンク先", "target" -> "開く場所", "rel" -> "リンク先との関係", "download" -> "ダウンロードさせる", "hreflang" -> "リンク先の言語"),
    "img" -> List("src" -> "画像のURL", "alt" -> "代替テキスト", "width" -> "幅", "height" -> "高さ", "loading" -> "読み込みのタイミング",
      "srcset" -> "解像度ごとの画像", "sizes" -> "表示サイズ", "decoding" -> "デコードの方法"),
    "input" -> List("type" -> "入力の種類", "name" -> "送信時の名前", "value" -> "値", "placeholder" -> "入力例", "disabled" -> "無効",
      "readonly" -> "読み取り専用", "required" -> "必須", "checked" -> "チェック済み", "min" -> "最小値", "max" -> "最大値", "step" -> "刻み",
      "maxlength" -> "最大の文字数", "minlength" -> "最小の文字数", "pattern" -> "入力の正規表現", "autocomplete" -> "自動補完",
      "list" -> "datalistのid", "multiple" -> "複数選択", "accept" -> "受け付けるファイルの種類", "form" -> "所属するformのid", "size" -> "表示の幅"),
    "button" -> List("type" -> "ボタンの種類", "disabled" -> "無効", "name" -> "送信時の名前", "value" -> "送信する値", "form" -> "所属するformのid",
      "popovertarget" -> "開くポップオーバーのid"),
    "form" -> List("action" -> "送信先", "method" -> "送信の方法", "enctype" -> "送信の形式", "novalidate" -> "検証しない", "target" -> "結果を開く場所",
      "autocomplete" -> "自動補完", "name" -> "フォームの名前"),
    "label" -> List("for" -> "対応する入力欄のid"),
    "select" -> List("name" -> "送信時の名前", "disabled" -> "無効", "required" -> "必須", "multiple" -> "複数選択", "size" -> "表示する行数"),
    "option" -> List("value" -> "値", "selected" -> "選択済み", "disabled" -> "無効", "label" -> "表示名"),
    "optgroup" -> List("label" -> "グループ名", "disabled" -> "無効"),
    "textarea" -> List("name" -> "送信時の名前", "placeholder" -> "入力例", "rows" -> "行数", "cols" -> "桁数", "disabled" -> "無効",
      "readonly" -> "読み取り専用", "required" -> "必須", "maxlength" -> "最大の文字数", "wrap" -> "折り返し"),
    "td" -> List("colspan" -> "列の結合", "rowspan" -> "行の結合", "headers" -> "見出しセルのid"),
    "th" -> List("colspan" -> "列の結合", "rowspan" -> "行の結合", "scope" -> "見出しの範囲", "abbr" -> "短い見出し"),
    "ol" -> List("start" -> "開始番号", "reversed" -> "逆順", "type" -> "番号の種類"),
    "li" -> List("value" -> "番号"),
    "iframe" -> List("src" -> "URL", "width" -> "幅", "height" -> "高さ", "allow" -> "許可する機能", "loading" -> "読み込みのタイミング",
      "sandbox" -> "制限", "referrerpolicy" -> "リファラー"),
    "video" -> List("src" -> "URL", "controls" -> "操作パネル", "autoplay" -> "自動再生", "loop" -> "繰り返し", "muted" -> "消音",
      "poster" -> "再生前の画像", "width" -> "幅", "height" -> "高さ", "playsinline" -> "インラインで再生", "preload" -> "事前の読み込み"),
    "audio" -> List("src" -> "URL", "controls" -> "操作パネル", "autoplay" -> "自動再生", "loop" -> "繰り返し", "muted" -> "消音", "preload" -> "事前の読み込み"),
    "source" -> List("src" -> "URL", "type" -> "MIMEタイプ", "srcset" -> "解像度ごとの画像", "media" -> "メディアクエリ"),
    "details" -> List("open" -> "開いている", "name" -> "同時に1つだけ開くグループ名"),
    "dialog" -> List("open" -> "開いている"),
    "progress" -> List("value" -> "現在の値", "max" -> "最大値"),
    "meter" -> List("value" -> "値", "min" -> "最小値", "max" -> "最大値", "low" -> "低い", "high" -> "高い", "optimum" -> "最適な値"),
    "time" -> List("datetime" -> "機械可読な日時"),
    "canvas" -> List("width" -> "幅", "height" -> "高さ"),
    "fieldset" -> List("disabled" -> "無効", "name" -> "名前", "form" -> "所属するformのid"),
    "output" -> List("for" -> "計算に使う要素のid", "name" -> "名前"),
    "blockquote" -> List("cite" -> "引用元のURL"),
    "q" -> List("cite" -> "引用元のURL"),
    "col" -> List("span" -> "列の数"),
    "colgroup" -> List("span" -> "列の数"),
    "data" -> List("value" -> "機械可読な値"),
  )

  /** よく使うイベント(@の後) */
  val events: List[(String, String)] = List(
    "click" -> "クリック", "dblclick" -> "ダブルクリック", "input" -> "入力", "change" -> "値の確定", "submit" -> "フォームの送信",
    "keydown" -> "キーを押した", "keyup" -> "キーを離した", "focus" -> "フォーカスを得た", "blur" -> "フォーカスを失った",
    "focusin" -> "フォーカスを得た(伝播する)", "focusout" -> "フォーカスを失った(伝播する)", "mouseenter" -> "マウスが入った",
    "mouseleave" -> "マウスが出た", "mouseover" -> "マウスが乗った", "mouseout" -> "マウスが離れた", "mousedown" -> "マウスのボタンを押した",
    "mouseup" -> "マウスのボタンを離した", "mousemove" -> "マウスの移動", "contextmenu" -> "右クリック", "wheel" -> "ホイール",
    "pointerdown" -> "ポインターを押した", "pointerup" -> "ポインターを離した", "pointermove" -> "ポインターの移動",
    "touchstart" -> "タッチの開始", "touchend" -> "タッチの終了", "scroll" -> "スクロール", "dragstart" -> "ドラッグの開始",
    "dragover" -> "ドラッグ中に重なった", "drop" -> "ドロップ", "load" -> "読み込みの完了", "error" -> "読み込みの失敗",
    "toggle" -> "detailsの開閉", "reset" -> "フォームのリセット", "copy" -> "コピー", "paste" -> "貼り付け",
  )

  /** 列挙された属性値 ((タグ, 属性) → 値). タグが"*"ならすべての要素 */
  val attributeValues: Map[(String, String), List[String]] = Map(
    ("input", "type") -> List("text", "password", "email", "number", "tel", "url", "search", "checkbox", "radio", "date",
      "datetime-local", "time", "month", "week", "color", "range", "file", "hidden", "submit", "button", "reset", "image"),
    ("button", "type") -> List("button", "submit", "reset"),
    ("a", "target") -> List("_blank", "_self", "_parent", "_top"),
    ("form", "target") -> List("_blank", "_self", "_parent", "_top"),
    ("a", "rel") -> List("noopener", "noreferrer", "nofollow", "external", "help", "license", "next", "prev"),
    ("form", "method") -> List("get", "post", "dialog"),
    ("form", "enctype") -> List("application/x-www-form-urlencoded", "multipart/form-data", "text/plain"),
    ("img", "loading") -> List("lazy", "eager"),
    ("iframe", "loading") -> List("lazy", "eager"),
    ("img", "decoding") -> List("async", "sync", "auto"),
    ("textarea", "wrap") -> List("soft", "hard"),
    ("th", "scope") -> List("row", "col", "rowgroup", "colgroup"),
    ("ol", "type") -> List("1", "a", "A", "i", "I"),
    ("video", "preload") -> List("none", "metadata", "auto"),
    ("audio", "preload") -> List("none", "metadata", "auto"),
    ("input", "autocomplete") -> List("on", "off", "name", "email", "username", "current-password", "new-password", "tel",
      "street-address", "postal-code", "country", "one-time-code"),
    ("*", "dir") -> List("ltr", "rtl", "auto"),
    ("*", "draggable") -> List("true", "false"),
    ("*", "contenteditable") -> List("true", "false", "plaintext-only"),
    ("*", "translate") -> List("yes", "no"),
    ("*", "spellcheck") -> List("true", "false"),
    ("*", "popover") -> List("auto", "manual"),
    ("*", "inputmode") -> List("none", "text", "decimal", "numeric", "tel", "search", "email", "url"),
    ("*", "enterkeyhint") -> List("enter", "done", "go", "next", "previous", "search", "send"),
    ("*", "aria-hidden") -> List("true", "false"),
    ("*", "aria-expanded") -> List("true", "false"),
    ("*", "role") -> List("button", "link", "dialog", "alert", "navigation", "main", "banner", "contentinfo", "tab", "tablist",
      "tabpanel", "menu", "menuitem", "list", "listitem", "checkbox", "switch", "img", "presentation", "none", "status"),
  )

  def attributesOf(tag: String): List[(String, String)] = elementAttributes.getOrElse(tag.toLowerCase, Nil)

  def valuesOf(tag: String, attr: String): List[String] = {
    val a = attr.stripPrefix(":").stripPrefix("v-bind:")
    attributeValues.get((tag.toLowerCase, a)).orElse(attributeValues.get(("*", a))).getOrElse(Nil)
  }

  /** 属性の説明(要素ごと → 全体の順) */
  def describeAttribute(tag: String, attr: String): Option[String] = {
    attributesOf(tag).toMap.get(attr).orElse(globalAttributes.toMap.get(attr))
  }

}
