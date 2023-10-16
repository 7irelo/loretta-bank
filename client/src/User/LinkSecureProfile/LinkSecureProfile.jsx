import styles from "./LinkSecureProfile.module.css"

function LinkSecureProfile() {
  return (
    <div className={styles.card}>
      <div className={styles.img}>
      </div>
      <div className={styles.text}>
        <h2>Profile Linked & identity confirmed</h2>
        <p>Your identity has been confirmed. Your banking profile is linked.</p>
      </div>
      
    </div>
  );
}

export default LinkSecureProfile;
