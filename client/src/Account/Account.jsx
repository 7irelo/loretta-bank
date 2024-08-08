import PropTypes from 'prop-types';
import styles from './Account.module.css';
import { useState, useEffect, useRef } from 'react';

function Account({ account }) {
  const [dropdownVisible, setDropdownVisible] = useState({
    pay: false,
    buy: false,
    manage: false
  });

  const dropdownRefs = {
    pay: useRef(null),
    buy: useRef(null),
    manage: useRef(null)
  };

  const handleDropdownToggle = (key) => {
    setDropdownVisible(prevState => {
      const newState = {
        pay: false,
        buy: false,
        manage: false
      };
      newState[key] = !prevState[key];
      return newState;
    });
  };

  const handleClickOutside = (event) => {
    for (const key in dropdownRefs) {
      if (
        dropdownRefs[key].current &&
        !dropdownRefs[key].current.contains(event.target) &&
        !event.target.closest(`.${styles[key]}`)
      ) {
        setDropdownVisible(prevState => ({
          ...prevState,
          [key]: false
        }));
      }
    }
  };

  useEffect(() => {
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  if (!account) {
    return <div>Account data is not available</div>;
  }

  const accountNumber = account.accountNumber ? account.accountNumber.replaceAll(' ', '-') : 'N/A';
  const userName = account.user ? `${account.user.lastName.toUpperCase()} ${account.user.firstName.charAt(0)}` : 'N/A';

  return (
    <div className={styles.container} id="card">
      <div className={styles.accountDetails}>
        <img src={`./src/assets/${account.accountType.toLowerCase()}.png`} alt="account picture" className={styles.accountImage} />
        <div className={styles.accountDescription}>
          <p>{account.name}</p>
          <p><small>Current Account {`${accountNumber.slice(0, 2)}-${accountNumber.slice(2, 4)}-${accountNumber.slice(4, 7)}-${accountNumber.slice(7, 11)}`}</small></p>
          <small>Name: {userName}</small>
        </div>
      </div>
      <hr />
      <div className={styles.accountBalance}>
        <div>
          <p>Available balance</p>
          <p>R{account.availableBalance}</p>
        </div>
        <div>
          <p>Latest balance</p>
          <p>R{account.latestBalance}</p>
        </div>
      </div>
      <hr />
      <div className={styles.accountButtons}>
        <div className={`${styles.buttonContainer} ${styles.pay}`}>
          <a><button className={`${styles.accountBtn}`} onClick={() => handleDropdownToggle('pay')}>PAY ▾</button></a>
          <div className={`${styles.payDropDown} ${dropdownVisible.pay ? styles.show : ''}`} ref={dropdownRefs.pay}>
            <ul>
              <li><a>Beneficiary</a></li>
              <li><a href="#">Once-off payment</a></li>
              <li><a href="#">Scheduled payment</a></li>
              <li><a href="#">Multiple beneficiaries</a></li>
              <li><a>Instant money</a></li>
              <li><a>More</a></li>
            </ul>
          </div>
        </div>
        <a><button className={`${styles.accountBtn} ${styles.transfer}`}>TRANSFER</button></a>
        <div className={`${styles.buttonContainer} ${styles.buy}`}>
          <a><button className={`${styles.accountBtn}`} onClick={() => handleDropdownToggle('buy')}>BUY ▾</button></a>
          <div className={`${styles.buyDropDown} ${dropdownVisible.buy ? styles.show : ''}`} ref={dropdownRefs.buy}>
            <ul>
              <li><a>Airtime</a></li>
              <li><a>Data bundles</a></li>
              <li><a>SMS bundles</a></li>
              <li><a>Electricity</a></li>
              <li><a>Lotto</a></li>
            </ul>
          </div>
        </div>
        <div className={`${styles.buttonContainer} ${styles.manage}`}>
          <a><button className={`${styles.accountBtn}`} onClick={() => handleDropdownToggle('manage')}>MANAGE ▾</button></a>
          <div className={`${styles.manageDropDown} ${dropdownVisible.manage ? styles.show : ''}`} ref={dropdownRefs.manage}>
            <ul>
              <li><a>Limits & card limits</a></li>
              <li><a>Beneficiaries</a></li>
              <li><a>Overdraft limit</a></li>
              <li><a>More</a></li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

Account.propTypes = {
  account: PropTypes.object.isRequired
};

export default Account;
