import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCheck, faTimes } from '@fortawesome/free-solid-svg-icons';
import styles from "./MyDashboards.module.css";

function MyDashboards() {
  return (
    <div className={styles.card}>
      <div className={styles.dashboardsHead}>
        <h2 className={styles.dashboardsHeader}>My Dashboards</h2>
        <button className={styles.addDashboardBtn}>ADD DASHBOARD</button>
      </div>
      <table className={styles.dashboardTable}>
        <thead>
          <tr>
            <th>Dashboard name</th>
            <th>Card number</th>
            <th>Card status</th>
            <th>Current Dashboard</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>My personal dashboard</td>
            <td>5678901256789012</td>
            <td>Active</td>
            <td><FontAwesomeIcon icon={faCheck} /></td>
          </tr>
          <tr>
            <td>Savings dashboard</td>
            <td>6789012367890123</td>
            <td>Inactive</td>
            <td><FontAwesomeIcon icon={faTimes} /></td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

export default MyDashboards;
