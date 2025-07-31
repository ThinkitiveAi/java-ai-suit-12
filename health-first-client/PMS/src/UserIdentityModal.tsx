import React from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Fade,
  Modal,
  useTheme
} from '@mui/material';
import { 
  LocalHospital, 
  Person,
  MedicalServices,
  HealthAndSafety
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

const StyledModal = styled(Modal)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: theme.spacing(2),
}));

const ModalCard = styled(Card)(({ theme }) => ({
  maxWidth: 600,
  width: '100%',
  borderRadius: theme.spacing(3),
  boxShadow: theme.shadows[12],
  background: 'rgba(255, 255, 255, 0.95)',
  backdropFilter: 'blur(10px)',
  border: '1px solid rgba(255, 255, 255, 0.2)',
}));

const IdentityCard = styled(Card)(({ theme }) => ({
  cursor: 'pointer',
  transition: 'all 0.3s ease',
  border: '2px solid transparent',
  background: 'rgba(255, 255, 255, 0.8)',
  '&:hover': {
    transform: 'translateY(-4px)',
    boxShadow: theme.shadows[8],
    borderColor: theme.palette.primary.main,
  },
}));

interface UserIdentityModalProps {
  open: boolean;
  onSelectProvider: () => void;
  onSelectPatient: () => void;
}

const UserIdentityModal: React.FC<UserIdentityModalProps> = ({
  open,
  onSelectProvider,
  onSelectPatient,
}) => {
  const theme = useTheme();

  return (
    <StyledModal open={open} disableEscapeKeyDown>
      <Fade in={open} timeout={800}>
        <ModalCard>
          <CardContent sx={{ p: 4, textAlign: 'center' }}>
            {/* Header */}
            <Box sx={{ mb: 4 }}>
              <LocalHospital 
                sx={{ 
                  fontSize: 60, 
                  color: theme.palette.primary.main,
                  mb: 2,
                  filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.1))'
                }} 
              />
              <Typography variant="h4" fontWeight={700} color="primary" gutterBottom>
                Welcome to MedCare Hospital
              </Typography>
              <Typography variant="h6" color="text.secondary" sx={{ mb: 1 }}>
                Patient Management System
              </Typography>
              <Typography variant="body1" color="text.secondary">
                Please select your identity to continue
              </Typography>
            </Box>

            {/* Identity Options */}
            <Box sx={{ display: 'flex', gap: 3, flexDirection: { xs: 'column', sm: 'row' } }}>
              {/* Provider Option */}
              <IdentityCard 
                onClick={onSelectProvider}
                sx={{ flex: 1, p: 3 }}
              >
                <Box sx={{ textAlign: 'center' }}>
                  <MedicalServices 
                    sx={{ 
                      fontSize: 48, 
                      color: theme.palette.primary.main,
                      mb: 2,
                      filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))'
                    }} 
                  />
                  <Typography variant="h5" fontWeight={600} color="primary" gutterBottom>
                    Healthcare Provider
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Medical professionals, doctors, nurses, and healthcare staff
                  </Typography>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Access patient records, manage appointments, and provide care
                  </Typography>
                </Box>
              </IdentityCard>

              {/* Patient Option */}
              <IdentityCard 
                onClick={onSelectPatient}
                sx={{ flex: 1, p: 3 }}
              >
                <Box sx={{ textAlign: 'center' }}>
                  <Person 
                    sx={{ 
                      fontSize: 48, 
                      color: theme.palette.secondary.main,
                      mb: 2,
                      filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))'
                    }} 
                  />
                  <Typography variant="h5" fontWeight={600} color="secondary" gutterBottom>
                    Patient
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Patients seeking medical care and health services
                  </Typography>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Book appointments, view medical records, and manage health
                  </Typography>
                </Box>
              </IdentityCard>
            </Box>

            {/* Footer */}
            <Box sx={{ mt: 4, pt: 3, borderTop: '1px solid', borderColor: 'divider' }}>
              <Typography variant="caption" color="text.secondary">
                Secure • HIPAA Compliant • 24/7 Access
              </Typography>
            </Box>
          </CardContent>
        </ModalCard>
      </Fade>
    </StyledModal>
  );
};

export default UserIdentityModal; 