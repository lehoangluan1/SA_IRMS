import { defineConfig } from "@lovable.dev/vite-tanstack-config";
import { nitro } from "nitro/vite";

export default defineConfig({
  vite: {
    plugins: [nitro()],
    server: {
      host: "0.0.0.0",
      port: 5173,
      strictPort: true,
    },
  },
});
