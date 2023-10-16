import styles from './Borrow.module.css';
import { useState } from 'react';
function Borrow() {

  const [isOn, setIsOn] = useState(true);
  const [namesArray, setNamesArray] = useState(["Eric", "Tirelo"]);

  const [contact, setContact] = useState({
    name: "Eric",
    surname: "Ncube",
    isFav: true
  });

  const icon = contact.isFav ? "savings" : "cheque"
  function flipSwitch() {
    setIsOn(prevState => !prevState)
  }
  function addName() {
    setNamesArray(preState => [...preState, "Surname"])
  }

  function flipFave() {
    setContact(prevState => ({
      ...prevState,
      isFav: !prevState.isFav
    }))
  }
  const nameElements = namesArray.map(name => <li key={name}>{name}</li>)
  return (
    <div>
      <h2>Borrow</h2>
      <div onClick={flipSwitch} className={styles.box}>
        <h1>{isOn ? "Yes" : "No"}</h1>
      </div>
      <button onClick={addName}>Add Name</button>
      <div>{nameElements}</div>
      <div>
        <img src={`src/assets/${icon}.png`} className={styles.pic} onClick={flipFave}/>
        <p1>{contact.name}</p1>
        <p1>{contact.surname}</p1>
      </div>
    </div>
  );
}

export default Borrow;
