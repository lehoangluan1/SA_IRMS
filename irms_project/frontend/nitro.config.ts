import { defineNitroConfig } from "nitro/config";

const apiProxyTarget = process.env.NITRO_API_PROXY_TARGET ?? "http://api-gateway:8080";

export default defineNitroConfig({
  routeRules: {
    "/api/**": {
      proxy: `${apiProxyTarget}/api/**`,
    },
    "/**": {
      headers: {
        "Cache-Control": "no-store",
      },
    },
  },
});
