import axios from "axios";

const isDev = import.meta.env.DEV;

/** 后端统一返回结构 */
export interface ApiResult<T = any> {
  code: number;
  data: T;
  description?: string;
  message?: string;
}

const instance = axios.create({
    baseURL: isDev ? 'http://localhost:8080/api' : import.meta.env.VITE_API_BASE_URL,
});

instance.defaults.withCredentials = true; // 配置为true

// Add a request interceptor
instance.interceptors.request.use(function (config) {
    console.log('我要发请求啦', config)
    // Do something before request is sent
    return config;
}, function (error) {
    // Do something with request error
    return Promise.reject(error);
});

// Add a response interceptor
instance.interceptors.response.use(function (response) {
    console.log('我收到你的响应啦', response)
    // 未登录则跳转到登录页
    if (response?.data?.code === 40100) {
        const redirectUrl = window.location.href;
        window.location.href = `/user/login?redirect=${redirectUrl}`;
    }
    // Do something with response data
    return response.data;
}, function (error) {
    // Do something with response error
    return Promise.reject(error);
});

// 响应拦截器已把数据解包（返回 response.data），这里做类型断言：
// 让 get/post 返回 ApiResult 而不是 AxiosResponse，避免业务代码里到处报 res.code 不存在
const myAxios = instance as unknown as {
  get<T = any>(url: string, config?: any): Promise<ApiResult<T>>;
  post<T = any>(url: string, data?: any, config?: any): Promise<ApiResult<T>>;
};

export default myAxios;
