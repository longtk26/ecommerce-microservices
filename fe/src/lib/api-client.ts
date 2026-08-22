export type TApiError = {
  status: number;
  message: string;
  data?: unknown;
};

class ApiClient {
  private async request<T>(url: string, options?: RequestInit): Promise<T> {
    const defaultHeaders: HeadersInit = {
      "Content-Type": "application/json",
      Accept: "application/json",
    };

    const response = await fetch(url, {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options?.headers,
      },
    });

    if (!response.ok) {
      let errorMessage = `HTTP error ${response.status} (${response.statusText})`;
      let errorData: unknown = null;
      try {
        errorData = await response.json();
        if (errorData && typeof errorData === "object" && "message" in errorData) {
          errorMessage = String(errorData.message);
        }
      } catch {
        // Response is not JSON
      }

      const error: TApiError = {
        status: response.status,
        message: errorMessage,
        data: errorData,
      };
      throw error;
    }

    if (response.status === 204 || response.status === 202) {
      return null as T;
    }

    const text = await response.text();
    if (!text || !text.trim()) {
      return null as T;
    }

    try {
      return JSON.parse(text) as T;
    } catch {
      return text as unknown as T;
    }
  }

  public get<T>(url: string, params?: Record<string, string | number | boolean | undefined>): Promise<T> {
    let finalUrl = url;
    if (params) {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined) {
          searchParams.append(key, String(value));
        }
      });
      const query = searchParams.toString();
      if (query) {
        finalUrl += (url.includes("?") ? "&" : "?") + query;
      }
    }
    return this.request<T>(finalUrl, { method: "GET" });
  }

  public post<T>(url: string, body?: unknown): Promise<T> {
    return this.request<T>(url, {
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    });
  }
}

export const apiClient = new ApiClient();
