import type { NextConfig } from "next";

const inventoryUrl = process.env.INVENTORY_SERVICE_URL || "http://localhost:8082";
const orderUrl = process.env.ORDER_SERVICE_URL || "http://localhost:8081";
const paymentUrl = process.env.PAYMENT_SERVICE_URL || "http://localhost:8083";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/inventory/:path*",
        destination: `${inventoryUrl}/api/:path*`,
      },
      {
        source: "/api/orders/:path*",
        destination: `${orderUrl}/api/orders/:path*`,
      },
      {
        source: "/api/orders",
        destination: `${orderUrl}/api/orders`,
      },
      {
        source: "/api/payments/:path*",
        destination: `${paymentUrl}/api/payments/:path*`,
      },
      {
        source: "/api/payments",
        destination: `${paymentUrl}/api/payments`,
      },
    ];
  },
};

export default nextConfig;
