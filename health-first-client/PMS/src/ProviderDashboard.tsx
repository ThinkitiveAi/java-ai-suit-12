import React, { useState } from 'react';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  Typography,
  Divider,
  IconButton,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Menu,
  MenuItem,
  useTheme,
  Fade,
  Paper,
  Grid,
  Card,
  CardContent,
  Chip,
  LinearProgress
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard,
  People,
  Event,
  MedicalServices,
  Settings,
  Notifications,
  AccountCircle,
  Logout,
  CalendarToday,
  Schedule,
  LocalHospital,
  TrendingUp,
  Assignment
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';
import ProviderAvailability from './ProviderAvailability';

const drawerWidth = 280;

const Main = styled('main', { shouldForwardProp: (prop) => prop !== 'open' })<{
  open?: boolean;
}>(({ theme, open }) => ({
  flexGrow: 1,
  padding: theme.spacing(3),
  transition: theme.transitions.create('margin', {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
  marginLeft: `-${drawerWidth}px`,
  ...(open && {
    transition: theme.transitions.create('margin', {
      easing: theme.transitions.easing.easeOut,
      duration: theme.transitions.duration.enteringScreen,
    }),
    marginLeft: 0,
  }),
}));

const DrawerHeader = styled('div')(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: theme.spacing(0, 1),
  ...theme.mixins.toolbar,
  justifyContent: 'flex-end',
}));

const StyledDrawer = styled(Drawer)(({ theme }) => ({
  width: drawerWidth,
  flexShrink: 0,
  '& .MuiDrawer-paper': {
    width: drawerWidth,
    boxSizing: 'border-box',
    background: '#ffffff', // Clean white background
    color: '#333333', // Dark gray text
    borderRight: '1px solid #e0e0e0', // Light gray border
  },
}));

const StyledAppBar = styled(AppBar, {
  shouldForwardProp: (prop) => prop !== 'open',
})<{ open?: boolean }>(({ theme, open }) => ({
  transition: theme.transitions.create(['margin', 'width'], {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
  background: '#ffffff', // Clean white background
  color: '#333333', // Dark gray text
  boxShadow: '0 2px 8px rgba(0,0,0,0.1)', // Subtle shadow
  ...(open && {
    width: `calc(100% - ${drawerWidth}px)`,
    marginLeft: `${drawerWidth}px`,
    transition: theme.transitions.create(['margin', 'width'], {
      easing: theme.transitions.easing.easeOut,
      duration: theme.transitions.duration.enteringScreen,
    }),
  }),
}));

const menuItems = [
  { text: 'Dashboard', icon: <Dashboard />, active: true },
  { text: 'Patient Management', icon: <People />, active: false },
  { text: 'Appointments', icon: <Event />, active: false },
  { text: 'Availability', icon: <Schedule />, active: false },
  { text: 'Medical Records', icon: <MedicalServices />, active: false },
  { text: 'Reports', icon: <TrendingUp />, active: false },
  { text: 'Settings', icon: <Settings />, active: false },
];

const ProviderDashboard: React.FC = () => {
  const [open, setOpen] = useState(true);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedMenuItem, setSelectedMenuItem] = useState(0);
  const theme = useTheme();

  const handleDrawerToggle = () => {
    setOpen(!open);
  };

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setAnchorEl(null);
  };

  const handleMenuItemClick = (index: number) => {
    setSelectedMenuItem(index);
    // Here we'll add navigation logic for different modules
  };

  const renderMainContent = () => {
    switch (selectedMenuItem) {
      case 0: // Dashboard
        return <DashboardContent />;
      case 1: // Patient Management
        return <PatientManagementContent />;
      case 2: // Appointments
        return <AppointmentsContent />;
      case 3: // Availability
        return <AvailabilityContent />;
      case 4: // Medical Records
        return <MedicalRecordsContent />;
      case 5: // Reports
        return <ReportsContent />;
      case 6: // Settings
        return <SettingsContent />;
      default:
        return <DashboardContent />;
    }
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <StyledAppBar position="fixed" open={open}>
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            onClick={handleDrawerToggle}
            edge="start"
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            MedCare Provider Portal
          </Typography>
          <IconButton color="inherit">
            <Notifications />
          </IconButton>
          <IconButton
            color="inherit"
            onClick={handleProfileMenuOpen}
          >
            <AccountCircle />
          </IconButton>
          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleProfileMenuClose}
          >
            <MenuItem onClick={handleProfileMenuClose}>Profile</MenuItem>
            <MenuItem onClick={handleProfileMenuClose}>Settings</MenuItem>
            <Divider />
            <MenuItem onClick={handleProfileMenuClose}>
              <Logout sx={{ mr: 1 }} />
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </StyledAppBar>

      <StyledDrawer
        variant="persistent"
        anchor="left"
        open={open}
      >
        <DrawerHeader>
          <Box sx={{ display: 'flex', alignItems: 'center', width: '100%', px: 2 }}>
            <LocalHospital sx={{ fontSize: 32, mr: 1, color: '#666666' }} />
            <Typography variant="h6" fontWeight={600} sx={{ color: '#333333' }}>
              MedCare
            </Typography>
          </Box>
        </DrawerHeader>
        <Divider sx={{ bgcolor: '#e0e0e0' }} />
        
        <Box sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Avatar sx={{ mr: 2, bgcolor: '#f0f0f0', color: '#666666' }}>
              <AccountCircle />
            </Avatar>
            <Box>
              <Typography variant="subtitle1" fontWeight={600} sx={{ color: '#333333' }}>
                Dr. Sarah Johnson
              </Typography>
              <Typography variant="caption" sx={{ color: '#666666' }}>
                Cardiologist
              </Typography>
            </Box>
          </Box>
        </Box>

        <List>
          {menuItems.map((item, index) => (
            <ListItem key={item.text} disablePadding>
              <ListItemButton
                onClick={() => handleMenuItemClick(index)}
                sx={{
                  mx: 1,
                  borderRadius: 1,
                  mb: 0.5,
                  bgcolor: selectedMenuItem === index ? '#f5f5f5' : 'transparent',
                  color: selectedMenuItem === index ? '#333333' : '#666666',
                  '&:hover': {
                    bgcolor: '#f0f0f0',
                  },
                }}
              >
                <ListItemIcon sx={{ 
                  color: selectedMenuItem === index ? '#333333' : '#666666', 
                  minWidth: 40 
                }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText primary={item.text} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </StyledDrawer>

      <Main open={open}>
        <DrawerHeader />
        <Fade in timeout={500}>
          <Box>
            {renderMainContent()}
          </Box>
        </Fade>
      </Main>
    </Box>
  );
};

// Dashboard Content Component
const DashboardContent = () => {
  const theme = useTheme();
  
  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom sx={{ mb: 3 }}>
        Dashboard Overview
      </Typography>
      
      <Grid container spacing={3}>
        {/* Stats Cards */}
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card sx={{ 
            background: '#ffffff',
            color: '#333333',
            transition: 'transform 0.2s, box-shadow 0.2s',
            border: '1px solid #e0e0e0',
            '&:hover': { 
              transform: 'translateY(-4px)',
              boxShadow: '0 8px 25px rgba(0,0,0,0.1)'
            }
          }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight={700} sx={{ color: '#1976d2' }}>24</Typography>
                  <Typography variant="body2" sx={{ color: '#666666' }}>Today's Appointments</Typography>
                </Box>
                <Event sx={{ fontSize: 40, color: '#1976d2', opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card sx={{ 
            background: '#ffffff',
            color: '#333333',
            transition: 'transform 0.2s, box-shadow 0.2s',
            border: '1px solid #e0e0e0',
            '&:hover': { 
              transform: 'translateY(-4px)',
              boxShadow: '0 8px 25px rgba(0,0,0,0.1)'
            }
          }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight={700} sx={{ color: '#2e7d32' }}>156</Typography>
                  <Typography variant="body2" sx={{ color: '#666666' }}>Total Patients</Typography>
                </Box>
                <People sx={{ fontSize: 40, color: '#2e7d32', opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card sx={{ 
            background: '#ffffff',
            color: '#333333',
            transition: 'transform 0.2s, box-shadow 0.2s',
            border: '1px solid #e0e0e0',
            '&:hover': { 
              transform: 'translateY(-4px)',
              boxShadow: '0 8px 25px rgba(0,0,0,0.1)'
            }
          }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight={700} sx={{ color: '#ed6c02' }}>8</Typography>
                  <Typography variant="body2" sx={{ color: '#666666' }}>Pending Reports</Typography>
                </Box>
                <Assignment sx={{ fontSize: 40, color: '#ed6c02', opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card sx={{ 
            background: '#ffffff',
            color: '#333333',
            transition: 'transform 0.2s, box-shadow 0.2s',
            border: '1px solid #e0e0e0',
            '&:hover': { 
              transform: 'translateY(-4px)',
              boxShadow: '0 8px 25px rgba(0,0,0,0.1)'
            }
          }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" fontWeight={700} sx={{ color: '#9c27b0' }}>92%</Typography>
                  <Typography variant="body2" sx={{ color: '#666666' }}>Availability</Typography>
                </Box>
                <Schedule sx={{ fontSize: 40, color: '#9c27b0', opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Recent Activity */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Recent Activity
              </Typography>
              <Box sx={{ mt: 2 }}>
                {[1, 2, 3, 4, 5].map((item) => (
                  <Box key={item} sx={{ display: 'flex', alignItems: 'center', py: 1, borderBottom: '1px solid #f0f0f0' }}>
                    <Avatar sx={{ width: 32, height: 32, mr: 2, bgcolor: theme.palette.primary.main }}>
                      <People sx={{ fontSize: 16 }} />
                    </Avatar>
                    <Box sx={{ flexGrow: 1 }}>
                      <Typography variant="body2" fontWeight={500}>
                        Patient appointment scheduled
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        2 hours ago
                      </Typography>
                    </Box>
                    <Chip label="New" size="small" color="primary" />
                  </Box>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Quick Actions */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" fontWeight={600} gutterBottom>
                Quick Actions
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Chip 
                  icon={<Event />} 
                  label="Schedule Appointment" 
                  sx={{ mb: 1, width: '100%', justifyContent: 'flex-start' }}
                  clickable
                />
                <Chip 
                  icon={<People />} 
                  label="Add Patient" 
                  sx={{ mb: 1, width: '100%', justifyContent: 'flex-start' }}
                  clickable
                />
                <Chip 
                  icon={<Schedule />} 
                  label="Update Availability" 
                  sx={{ mb: 1, width: '100%', justifyContent: 'flex-start' }}
                  clickable
                />
                <Chip 
                  icon={<MedicalServices />} 
                  label="View Records" 
                  sx={{ mb: 1, width: '100%', justifyContent: 'flex-start' }}
                  clickable
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

// Placeholder components for other modules
const PatientManagementContent = () => (
  <Box>
    <Typography variant="h4" fontWeight={700} gutterBottom>
      Patient Management
    </Typography>
    <Typography variant="body1" color="text.secondary">
      Patient management module coming soon...
    </Typography>
  </Box>
);

const AppointmentsContent = () => (
  <Box>
    <Typography variant="h4" fontWeight={700} gutterBottom>
      Appointments
    </Typography>
    <Typography variant="body1" color="text.secondary">
      Appointments module coming soon...
    </Typography>
  </Box>
);

const AvailabilityContent = () => (
  <ProviderAvailability />
);

const MedicalRecordsContent = () => (
  <Box>
    <Typography variant="h4" fontWeight={700} gutterBottom>
      Medical Records
    </Typography>
    <Typography variant="body1" color="text.secondary">
      Medical records module coming soon...
    </Typography>
  </Box>
);

const ReportsContent = () => (
  <Box>
    <Typography variant="h4" fontWeight={700} gutterBottom>
      Reports
    </Typography>
    <Typography variant="body1" color="text.secondary">
      Reports module coming soon...
    </Typography>
  </Box>
);

const SettingsContent = () => (
  <Box>
    <Typography variant="h4" fontWeight={700} gutterBottom>
      Settings
    </Typography>
    <Typography variant="body1" color="text.secondary">
      Settings module coming soon...
    </Typography>
  </Box>
);

export default ProviderDashboard; 