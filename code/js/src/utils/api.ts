export class ApiError extends Error {
  status: number;
  body: any;

  constructor(message: string, status: number, body: any) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export const handleResponse = async <T>(res: Response): Promise<T> => {
  const contentType = res.headers.get("Content-Type") || "";
  let data: any;

  if (res.status === 204 || res.headers.get("content-length") === "0") {
    data = null;
  } else {
    try {
      data =
        contentType.includes("application/json") ||
        contentType.includes("application/problem+json")
          ? await res.json()
          : await res.text();
    } catch (error) {
      if (res.ok) {
        throw new Error("Invalid response from server");
      }
      data = null;
    }
  }

  if (!res.ok) {
    const message = data?.detail || "An error occurred";
    throw new ApiError(message, res.status, data);
  }

  return data as T;
};

export const apiRequest = async <T>(
  endpoint: string,
  options: RequestInit = {},
): Promise<T> => {
  const token = localStorage.getItem("token");

  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(endpoint, {
    ...options,
    headers,
    credentials: "include",
  });

  return handleResponse<T>(response);
};
