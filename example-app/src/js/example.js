import { EmailLink } from 'capacitor-email-link';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    EmailLink.echo({ value: inputValue })
}
