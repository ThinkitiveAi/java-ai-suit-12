import { useState } from 'react'
import './App.css'
import ProviderLogin from './ProviderLogin'
import PatientLogin from './PatientLogin'
import UserIdentityModal from './UserIdentityModal'

type UserType = 'provider' | 'patient' | null;

function App() {
  const [userType, setUserType] = useState<UserType>(null);
  const [showIdentityModal, setShowIdentityModal] = useState(true);

  const handleSelectProvider = () => {
    setUserType('provider');
    setShowIdentityModal(false);
  };

  const handleSelectPatient = () => {
    setUserType('patient');
    setShowIdentityModal(false);
  };

  const handleBackToIdentity = () => {
    setUserType(null);
    setShowIdentityModal(true);
  };
  return (
    <>
      <UserIdentityModal
        open={showIdentityModal}
        onSelectProvider={handleSelectProvider}
        onSelectPatient={handleSelectPatient}
      />
      
      {userType === 'provider' && (
        <ProviderLogin onBackToIdentity={handleBackToIdentity} />
      )}
      
      {userType === 'patient' && (
        <PatientLogin onBackToIdentity={handleBackToIdentity} />
      )}
    </>
  )
}

export default App
