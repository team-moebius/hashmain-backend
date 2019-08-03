import axios from 'axios';
import { push } from 'connected-react-router';

const ajax = axios.create({
  baseURL: 'http://52.79.86.26/api/',
  responseType: 'json',
});

const setAjaxJwtHeader = (jwtHeader: String) => {
  ajax.defaults.headers.common['Authorization'] = `Bearer ${jwtHeader}`;
};

const addSignOutInterceptor = (dispatchFunc: any, signOutFunc: any) => {
  ajax.interceptors.response.use(
    response => {
      return response;
    },
    error => {
      console.log(error);
      if (error.response.status === 401) {
        setAjaxJwtHeader('');
        alert('세션 만료. 재로그인 해주세요.');
        dispatchFunc(signOutFunc());
        push('http://localhost:3000/sign');
      }
      return Promise.reject(error);
    }
  );
};

export { ajax, setAjaxJwtHeader, addSignOutInterceptor };
export default ajax;
