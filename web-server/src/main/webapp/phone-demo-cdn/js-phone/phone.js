const input=document.querySelector('#phone');

const iti=window.intlTelInput(input,{
  initialCountry:'dk',
  preferredCountries:['dk','se','no','de','us'],
  separateDialCode:true,
  loadUtils:()=>import('https://cdn.jsdelivr.net/npm/intl-tel-input@25.12.2/build/js/utils.js')
});

const result=document.getElementById('result');

function validate(){
 if(!input.value.trim()){result.textContent='';return;}
 if(iti.isValidNumber()){
   result.innerHTML='✔ '+iti.getNumber();
   result.className='ok';
 }else{
   result.textContent='Ugyldigt telefonnummer';
   result.className='error';
 }
}

input.addEventListener('input',validate);
input.addEventListener('countrychange',validate);
