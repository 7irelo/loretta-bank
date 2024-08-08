import { Link, Route, Routes, Navigate } from 'react-router-dom';
import MyDashboards from './MyDashboards/MyDashboards';
import LinkSecureProfile from './LinkSecureProfile/LinkSecureProfile';
import ManageDetails from './ManageDetails/ManageDetails';
import OverdraftLimit from './OverdraftLimit/OverdraftLimit';
import ManageConsents from './ManageConsents/ManageConsents';
import ManageThirdPartyAccess from './ManageThirdPartyAccess/ManageThirdPartyAccess';
import StatementDeliveryAddress from './StatementDeliveryAddress/StatementDeliveryAddress';
import ManageDevices from './ManageDevices/ManageDevices';
import ViewPersonalDetails from './ViewPersonalDetails/ViewPersonalDetails';
import ManageOtpPreference from './ManageOtpPreference/ManageOtpPreference';
import styles from './User.module.css';

function User() {
  return (
    <div className={styles.userContainer}>
      <nav className={styles.sidebar}>
        <ul>
          <li>
            <h3>My Profile and settings</h3>
          </li>
          <li>
            <Link to="/user/dashboards"><a>My Dashboards</a></Link>
          </li>
          <li>
            <Link to="/user/link-secure"><a>Link & Secure Your Profile</a></Link>
          </li>
          <li>
            <Link to="/user/manage-details"><a>Manage Your Details</a></Link>
          </li>
          <li>
            <Link to="/user/overdraft-limit"><a>Overdraft Limit</a></Link>
          </li>
          <li>
            <Link to="/user/manage-consents"><a>Manage Your Consents</a></Link>
          </li>
          <li>
            <Link to="/user/manage-third-party"><a>Manage Third-Party Access</a></Link>
          </li>
          <li>
            <Link to="/user/statement-delivery"><a>Statement Delivery Address</a></Link>
          </li>
          <li>
            <Link to="/user/manage-devices"><a>Manage Devices</a></Link>
          </li>
          <li>
            <Link to="/user/view-personal-details"><a>View Personal Details</a></Link>
          </li>
          <li>
            <Link to="/user/manage-otp"><a>Manage OTP Preference</a></Link>
          </li>
        </ul>
      </nav>
      <hr/>
      <div className={styles.content}>
        <Routes>
          <Route path="/dashboards" element={<MyDashboards />} />
          <Route path="/link-secure" element={<LinkSecureProfile />} />
          <Route path="/manage-details" element={<ManageDetails />} />
          <Route path="/overdraft-limit" element={<OverdraftLimit />} />
          <Route path="/manage-consents" element={<ManageConsents />} />
          <Route path="/manage-third-party" element={<ManageThirdPartyAccess />} />
          <Route path="/statement-delivery" element={<StatementDeliveryAddress />} />
          <Route path="/manage-devices" element={<ManageDevices />} />
          <Route path="/view-personal-details" element={<ViewPersonalDetails />} />
          <Route path="/manage-otp" element={<ManageOtpPreference />} />
          <Route path="*" element={<Navigate to="/user/dashboards" />} />
        </Routes>
      </div>
    </div>
  );
}

export default User;
