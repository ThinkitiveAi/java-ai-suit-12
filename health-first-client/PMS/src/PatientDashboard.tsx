import React, { useState } from 'react';
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Card,
  CardContent,
  Grid,
  IconButton,
  Menu,
  MenuItem,
  useTheme,
  useMediaQuery,
  Button
} from '@mui/material';
import {
  Person,
  Event,
  LocalHospital,
  Medication,
  Description,
  Settings,
  Logout,
  Menu as MenuIcon,
  CalendarToday,
  HealthAndSafety,
  Assignment
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

const drawerWidth = 240;

const StyledDrawer = styled(Drawer)(({ theme }) => ({
  width: drawerWidth,
  flexShrink: 0,
  '& .MuiDrawer-paper': {
    width: drawerWidth,
    boxSizing: 'border-box',
    background: '#ffffff',
    color: '#333333',
    borderRight: '1px solid #e0e0e0',
  },
}));

const StyledAppBar = styled(AppBar)(({ theme }) => ({
  background: '#ffffff',
  color: '#333333',
  boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
  zIndex: theme.zIndex.drawer + 1,
}));

const DashboardContent = styled(Box)(({ theme }) => ({
  flexGrow: 1,
  padding: theme.spacing(3),
  background: '#f5f5f5',
  minHeight: '100vh',
  [theme.breakpoints.down('md')]: {
    padding: theme.spacing(2),
  },
}));

const menuItems = [
  { text: 'Dashboard', icon: <Person />, id: 'dashboard' },
  { text: 'Appointments', icon: <Event />, id: 'appointments' },
  { text: 'Medical Records', icon: <Description />, id: 'records' },
  { text: 'Medications', icon: <Medication />, id: 'medications' },
  { text: 'Health Info', icon: <HealthAndSafety />, id: 'health' },
  { text: 'Settings', icon: <Settings />, id: 'settings' },
];

const PatientDashboard: React.FC = () => {
  const [selectedMenuItem, setSelectedMenuItem] = useState('dashboard');
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleMenuClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    // Handle logout logic
    console.log('Patient logged out');
  };

  const drawer = (
    <Box>
      <Box sx={{ p: 2, textAlign: 'center', borderBottom: '1px solid #e0e0e0' }}>
        <LocalHospital sx={{ fontSize: 40, color: '#4caf50', mb: 1 }} />
        <Typography variant="h6" fontWeight={700} sx={{ color: '#333333' }}>
          MedCare
        </Typography>
        <Typography variant="body2" sx={{ color: '#666666' }}>
          Patient Portal
        </Typography>
      </Box>
      <List sx={{ pt: 1 }}>
        {menuItems.map((item) => (
          <ListItem key={item.id} disablePadding>
            <ListItemButton
              selected={selectedMenuItem === item.id}
              onClick={() => setSelectedMenuItem(item.id)}
              sx={{
                mx: 1,
                borderRadius: 1,
                '&.Mui-selected': {
                  backgroundColor: '#e8f5e8',
                  color: '#4caf50',
                  '&:hover': {
                    backgroundColor: '#c8e6c9',
                  },
                },
                '&:hover': {
                  backgroundColor: '#f5f5f5',
                },
              }}
            >
              <ListItemIcon sx={{ 
                color: selectedMenuItem === item.id ? '#4caf50' : '#666666',
                minWidth: 40 
              }}>
                {item.icon}
              </ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <StyledAppBar position="fixed">
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Patient Dashboard
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Avatar sx={{ bgcolor: '#4caf50', width: 32, height: 32 }}>
              <Person />
            </Avatar>
            <IconButton
              color="inherit"
              onClick={handleMenuClick}
            >
              <Settings />
            </IconButton>
            <Menu
              anchorEl={anchorEl}
              open={Boolean(anchorEl)}
              onClose={handleMenuClose}
            >
              <MenuItem onClick={handleMenuClose}>Profile</MenuItem>
              <MenuItem onClick={handleMenuClose}>Settings</MenuItem>
              <MenuItem onClick={handleLogout}>
                <Logout sx={{ mr: 1 }} />
                Logout
              </MenuItem>
            </Menu>
          </Box>
        </Toolbar>
      </StyledAppBar>

      <StyledDrawer
        variant={isMobile ? 'temporary' : 'permanent'}
        open={isMobile ? mobileOpen : true}
        onClose={handleDrawerToggle}
        ModalProps={{
          keepMounted: true,
        }}
      >
        {drawer}
      </StyledDrawer>

      <DashboardContent>
        <Box sx={{ mt: 8, mb: 3 }}>
          <Typography variant="h4" fontWeight={700} sx={{ color: '#333333', mb: 1 }}>
            Welcome back, Patient!
          </Typography>
          <Typography variant="body1" sx={{ color: '#666666' }}>
            Here's your health overview for today
          </Typography>
        </Box>

        {/* Dashboard Stats */}
        <Grid container spacing={3} sx={{ mb: 4 }}>
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
                    <Typography variant="h4" fontWeight={700} sx={{ color: '#4caf50' }}>3</Typography>
                    <Typography variant="body2" sx={{ color: '#666666' }}>Upcoming Appointments</Typography>
                  </Box>
                  <Event sx={{ fontSize: 40, color: '#4caf50', opacity: 0.8 }} />
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
                    <Typography variant="h4" fontWeight={700} sx={{ color: '#2196f3' }}>12</Typography>
                    <Typography variant="body2" sx={{ color: '#666666' }}>Medical Records</Typography>
                  </Box>
                  <Description sx={{ fontSize: 40, color: '#2196f3', opacity: 0.8 }} />
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
                    <Typography variant="h4" fontWeight={700} sx={{ color: '#ff9800' }}>5</Typography>
                    <Typography variant="body2" sx={{ color: '#666666' }}>Active Medications</Typography>
                  </Box>
                  <Medication sx={{ fontSize: 40, color: '#ff9800', opacity: 0.8 }} />
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
                    <Typography variant="h4" fontWeight={700} sx={{ color: '#9c27b0' }}>2</Typography>
                    <Typography variant="body2" sx={{ color: '#666666' }}>Pending Tests</Typography>
                  </Box>
                  <Assignment sx={{ fontSize: 40, color: '#9c27b0', opacity: 0.8 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Quick Actions */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{
              background: '#ffffff',
              color: '#333333',
              border: '1px solid #e0e0e0',
              height: '100%'
            }}>
              <CardContent>
                <Typography variant="h6" fontWeight={600} sx={{ mb: 2, color: '#333333' }}>
                  Quick Actions
                </Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <Button
                    variant="outlined"
                    startIcon={<CalendarToday />}
                    sx={{ justifyContent: 'flex-start', color: '#4caf50', borderColor: '#4caf50' }}
                  >
                    Book New Appointment
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<Description />}
                    sx={{ justifyContent: 'flex-start', color: '#2196f3', borderColor: '#2196f3' }}
                  >
                    View Medical Records
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<Medication />}
                    sx={{ justifyContent: 'flex-start', color: '#ff9800', borderColor: '#ff9800' }}
                  >
                    Check Medications
                  </Button>
                </Box>
              </CardContent>
            </Card>
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <Card sx={{
              background: '#ffffff',
              color: '#333333',
              border: '1px solid #e0e0e0',
              height: '100%'
            }}>
              <CardContent>
                <Typography variant="h6" fontWeight={600} sx={{ mb: 2, color: '#333333' }}>
                  Recent Activity
                </Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Event sx={{ color: '#4caf50', fontSize: 20 }} />
                    <Box>
                      <Typography variant="body2" fontWeight={500}>Appointment scheduled</Typography>
                      <Typography variant="caption" sx={{ color: '#666666' }}>Dr. Smith - Tomorrow 2:00 PM</Typography>
                    </Box>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Description sx={{ color: '#2196f3', fontSize: 20 }} />
                    <Box>
                      <Typography variant="body2" fontWeight={500}>Lab results available</Typography>
                      <Typography variant="caption" sx={{ color: '#666666' }}>Blood test - 2 days ago</Typography>
                    </Box>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Medication sx={{ color: '#ff9800', fontSize: 20 }} />
                    <Box>
                      <Typography variant="body2" fontWeight={500}>Prescription renewed</Typography>
                      <Typography variant="caption" sx={{ color: '#666666' }}>Amoxicillin - 1 week ago</Typography>
                    </Box>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </DashboardContent>
    </Box>
  );
};

export default PatientDashboard; 