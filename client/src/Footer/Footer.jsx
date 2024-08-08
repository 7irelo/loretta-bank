import styles from "./Footer.module.css";
import logo from "../assets/logo.png";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCopyright } from '@fortawesome/free-solid-svg-icons';

function Footer() {
  const date = new Date()
  return (
    <footer className={styles.footer}>
      <img src={logo} alt="Loretta Bank Logo" className={styles.footerLogo} />
      <h4><FontAwesomeIcon icon={faCopyright}/> {date.getFullYear()} Loretta Bank</h4>
      <ul className={styles.footerLinks}>
        <li><a>Report fraud</a></li>
        <li><a>Report a problem</a></li>
      </ul>
    </footer>
  );
}

export default Footer;
