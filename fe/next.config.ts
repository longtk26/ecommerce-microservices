import type { NextConfig } from "next";

const apiGatewayUrl = process.env.API_GATEWAY_URL || "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      // Map legacy /api/inventory/:path* rewrite to /api/:path* on Gateway
      {
        source: "/api/inventory/:path*",
        destination: `${apiGatewayUrl}/api/:path*`,
      },
      // Direct gateway forwarding for all /api/* routes
      {
        source: "/api/:path*",
        destination: `${apiGatewayUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;

