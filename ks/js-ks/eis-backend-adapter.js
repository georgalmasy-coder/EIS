import {getChildText, parseXml} from './core/xml.js';

export const BACKEND_ENDPOINTS=Object.freeze({
  login:'./api/login',
  logout:'/api/logout',
  projectSelection:'/Menu',
  customerProjects:'/api/customersprojects',
  customers:'/api/admin/customers',
  projectOverview:'/project/overview?cmd=overview',
  menu:'/Menu'
});

export class BackendResponseError extends Error{
  constructor(endpoint,status){
    super(`EiS backend returned HTTP ${status} from ${endpoint}`);
    this.name='BackendResponseError';
    this.endpoint=endpoint;
    this.status=status;
  }
}

export async function fetchBackendXml(endpoint){
  const response=await fetch(endpoint,{
    method:'GET',
    headers:{Accept:'application/xml, text/xml'},
    credentials:'same-origin'
  });
  if(!response.ok)throw new BackendResponseError(endpoint,response.status);
  return parseXml(await response.text());
}

export function readShellContext(xmlDocument){
  const topPanel=xmlDocument?.querySelector?.('TopPanel');
  if(!topPanel)return null;
  const userName=getChildText(topPanel,'UserName','')||getChildText(topPanel,'Name','')||getChildText(topPanel,'User','');
  return {
    customerName:getChildText(topPanel,'CustomerName','')||getChildText(topPanel,'Customer',''),
    projectName:getChildText(topPanel,'ProjectName','')||getChildText(topPanel,'Project',''),
    userName
  };
}

export async function loadShellContext(){
  return readShellContext(await fetchBackendXml(BACKEND_ENDPOINTS.projectOverview));
}

export async function loadAdminCustomers(){
  const document=await fetchBackendXml(BACKEND_ENDPOINTS.customers);
  return Array.from(document.querySelectorAll('customers > customer')).map(node=>({
    id:getChildText(node,'customerId',''),
    name:getChildText(node,'customerName',''),
    admin:getChildText(node,'contactName','')||getChildText(node,'contactEmail',''),
    subscription:getChildText(node,'subscriptionName','')||getChildText(node,'subscriptionStatus',''),
    status:getChildText(node,'customerStatusLabel','')||getChildText(node,'customerStatus',''),
    changed:getChildText(node,'changedDateTime','')||getChildText(node,'createdDateTime','')
  })).filter(customer=>customer.id&&customer.name);
}
