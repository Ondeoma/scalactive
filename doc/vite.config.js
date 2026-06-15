import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";
import fs from "node:fs";
import path from "node:path";

export default defineConfig({
  plugins: [
    scalaJSPlugin({
      cwd: "..", // sbtプロジェクトのルート（build.sbtがある場所）を指定
      projectID: "doc", // sbt内のプロジェクトIDを指定
    }),
    {
      name: "copy-index-to-404",
      closeBundle() {
        const distDir = path.resolve(__dirname, "dist");
        const indexPath = path.resolve(distDir, "index.html");
        const path404 = path.resolve(distDir, "404.html");

        if (fs.existsSync(indexPath)) {
          fs.copyFileSync(indexPath, path404);
        }
      },
    },
  ],
});
