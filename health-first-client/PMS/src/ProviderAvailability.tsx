import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Checkbox,
  FormControlLabel,
  Chip,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  Tab,
  useTheme,
  Fade,
  Alert,
  Divider,
  Switch,
  Slider,
  InputAdornment,
  Tooltip,
  Snackbar,
  CircularProgress,
  Badge,
  useMediaQuery,
  Fab,
  SpeedDial,
  SpeedDialAction,
  SpeedDialIcon,
  Backdrop
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  CalendarToday,
  Schedule,
  LocationOn,
  AttachMoney,
  Notes,
  Save,
  Cancel,
  ViewWeek,
  ViewDay,
  ViewModule,
  Today,
  NavigateBefore,
  NavigateNext,
  Refresh,
  Settings,
  Warning,
  CheckCircle,
  Error,
  Info,
  Notifications,
  Close
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  title?: string;
  action?: string;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`availability-tabpanel-${index}`}
      aria-labelledby={`availability-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ p: { xs: 1, sm: 2, md: 3 } }}>
          {children}
        </Box>
      )}
    </div>
  );
}

const StyledCard = styled(Card)(({ theme }) => ({
  height: '100%',
  transition: 'transform 0.2s, box-shadow 0.2s',
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: theme.shadows[8],
  },
  [theme.breakpoints.down('sm')]: {
    marginBottom: theme.spacing(2),
  },
}));

const CalendarDay = styled(Box)(({ theme }) => ({
  border: '1px solid #e0e0e0',
  padding: theme.spacing(1),
  minHeight: { xs: 60, sm: 80 },
  cursor: 'pointer',
  transition: 'background-color 0.2s, transform 0.1s',
  '&:hover': {
    backgroundColor: '#f5f5f5',
    transform: 'scale(1.02)',
  },
  '&:active': {
    transform: 'scale(0.98)',
  },
  '&.selected': {
    backgroundColor: '#e3f2fd', // Light blue background
    color: '#1976d2', // Blue text
    border: '2px solid #1976d2',
  },
  '&.has-slots': {
    backgroundColor: '#e8f5e8', // Light green background
    color: '#2e7d32', // Green text
  },
  '&.conflict': {
    backgroundColor: '#ffebee', // Light red background
    color: '#d32f2f', // Red text
  },
  '&.limited': {
    backgroundColor: '#fff3e0', // Light orange background
    color: '#f57c00', // Orange text
  },
  [theme.breakpoints.down('sm')]: {
    padding: theme.spacing(0.5),
    minHeight: 50,
    fontSize: '0.8rem',
  },
}));

const ResponsiveButton = styled(Button)(({ theme }) => ({
  [theme.breakpoints.down('sm')]: {
    minHeight: 48,
    fontSize: '0.9rem',
    padding: theme.spacing(1, 2),
  },
}));

const timeSlots = [
  '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
  '12:00', '12:30', '13:00', '13:30', '14:00', '14:30',
  '15:00', '15:30', '16:00', '16:30', '17:00', '17:30'
];

const appointmentTypes = [
  'Consultation',
  'Follow-up',
  'Emergency',
  'Telemedicine'
];

const locationTypes = [
  'Clinic',
  'Hospital',
  'Telemedicine',
  'Home Visit'
];

const currencies = ['USD', 'EUR', 'GBP', 'CAD', 'AUD'];

const ProviderAvailability: React.FC = () => {
  const [tabValue, setTabValue] = useState(0);
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [dialogMode, setDialogMode] = useState<'add' | 'edit'>('add');
  const [selectedSlot, setSelectedSlot] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [openSpeedDial, setOpenSpeedDial] = useState(false);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const isTablet = useMediaQuery(theme.breakpoints.down('md'));

  // Form state
  const [formData, setFormData] = useState({
    provider: 'Dr. Sarah Johnson',
    date: '',
    startTime: '',
    endTime: '',
    timezone: 'America/New_York',
    isRecurring: false,
    recurrencePattern: 'weekly',
    recurrenceEndDate: '',
    slotDuration: 30,
    breakDuration: 0,
    maxAppointments: 1,
    appointmentType: 'Consultation',
    locationType: 'Clinic',
    address: '',
    roomNumber: '',
    baseFee: '',
    insuranceAccepted: false,
    currency: 'USD',
    notes: '',
    specialRequirements: []
  });

  const addNotification = (notification: Omit<Notification, 'id'>) => {
    const newNotification = {
      ...notification,
      id: Date.now().toString(),
    };
    setNotifications(prev => [...prev, newNotification]);
  };

  const removeNotification = (id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleDateSelect = (date: Date) => {
    setSelectedDate(date);
    setFormData(prev => ({
      ...prev,
      date: date.toISOString().split('T')[0]
    }));
    
    // Check for conflicts
    if (hasConflict(date)) {
      addNotification({
        type: 'warning',
        title: 'Scheduling Conflict',
        message: 'This date has potential scheduling conflicts. Please review before adding availability.',
      });
    }
  };

  const handleOpenDialog = (mode: 'add' | 'edit', slot?: any) => {
    setDialogMode(mode);
    if (slot) {
      setSelectedSlot(slot);
      setFormData(slot);
    } else {
      setSelectedSlot(null);
      setFormData({
        provider: 'Dr. Sarah Johnson',
        date: selectedDate ? selectedDate.toISOString().split('T')[0] : '',
        startTime: '',
        endTime: '',
        timezone: 'America/New_York',
        isRecurring: false,
        recurrencePattern: 'weekly',
        recurrenceEndDate: '',
        slotDuration: 30,
        breakDuration: 0,
        maxAppointments: 1,
        appointmentType: 'Consultation',
        locationType: 'Clinic',
        address: '',
        roomNumber: '',
        baseFee: '',
        insuranceAccepted: false,
        currency: 'USD',
        notes: '',
        specialRequirements: []
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setSelectedSlot(null);
  };

  const handleFormChange = (field: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleSave = async () => {
    setLoading(true);
    
    // Simulate API call
    setTimeout(() => {
      setLoading(false);
      handleCloseDialog();
      
      addNotification({
        type: 'success',
        title: 'Availability Updated',
        message: `Successfully ${dialogMode === 'add' ? 'added' : 'updated'} availability for ${formData.date}`,
      });
    }, 1500);
  };

  const generateCalendarDays = () => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());

    const days = [];
    for (let i = 0; i < 42; i++) {
      const date = new Date(startDate);
      date.setDate(startDate.getDate() + i);
      days.push(date);
    }
    return days;
  };

  const isCurrentMonth = (date: Date) => {
    return date.getMonth() === currentDate.getMonth();
  };

  const isToday = (date: Date) => {
    const today = new Date();
    return date.toDateString() === today.toDateString();
  };

  const hasAvailability = (date: Date) => {
    // Mock data - in real app this would check actual availability
    return Math.random() > 0.7;
  };

  const hasConflict = (date: Date) => {
    // Mock conflict detection
    return Math.random() > 0.8;
  };

  const getAvailabilityStatus = (date: Date) => {
    if (hasConflict(date)) return 'conflict';
    if (hasAvailability(date)) {
      return Math.random() > 0.5 ? 'has-slots' : 'limited';
    }
    return 'normal';
  };

  const navigateMonth = (direction: 'prev' | 'next') => {
    setCurrentDate(prev => {
      const newDate = new Date(prev);
      if (direction === 'prev') {
        newDate.setMonth(newDate.getMonth() - 1);
      } else {
        newDate.setMonth(newDate.getMonth() + 1);
      }
      return newDate;
    });
  };

  const getStatusIcon = (type: string) => {
    switch (type) {
      case 'success': return <CheckCircle />;
      case 'error': return <Error />;
      case 'warning': return <Warning />;
      case 'info': return <Info />;
      default: return <Info />;
    }
  };

  const getStatusColor = (type: string) => {
    switch (type) {
      case 'success': return 'success';
      case 'error': return 'error';
      case 'warning': return 'warning';
      case 'info': return 'info';
      default: return 'info';
    }
  };

  const speedDialActions = [
    { icon: <Add />, name: 'Add Slot', action: () => handleOpenDialog('add') },
    { icon: <Schedule />, name: 'Batch Add', action: () => addNotification({ type: 'info', message: 'Batch add feature coming soon!' }) },
    { icon: <Settings />, name: 'Settings', action: () => addNotification({ type: 'info', message: 'Settings panel coming soon!' }) },
  ];

  return (
    <Box>
      {/* Header with responsive design */}
      <Box sx={{ 
        display: 'flex', 
        flexDirection: { xs: 'column', sm: 'row' },
        justifyContent: 'space-between', 
        alignItems: { xs: 'stretch', sm: 'center' }, 
        mb: 3,
        gap: { xs: 2, sm: 0 }
      }}>
        <Typography variant={isMobile ? "h5" : "h4"} fontWeight={700}>
          Availability Management
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <ResponsiveButton
            variant="contained"
            startIcon={<Add />}
            onClick={() => handleOpenDialog('add')}
            sx={{ borderRadius: 2 }}
          >
            {isMobile ? 'Add' : 'Add Availability'}
          </ResponsiveButton>
          {!isMobile && (
            <ResponsiveButton
              variant="outlined"
              startIcon={<Refresh />}
              onClick={() => addNotification({ type: 'info', message: 'Refreshing availability data...' })}
            >
              Refresh
            </ResponsiveButton>
          )}
        </Box>
      </Box>

      {/* Main Content with responsive grid */}
      <Grid container spacing={{ xs: 2, md: 3 }}>
        {/* Calendar View */}
        <Grid size={{ xs: 12, lg: 8 }}>
          <StyledCard>
            <CardContent sx={{ p: { xs: 1, sm: 2, md: 3 } }}>
              <Box sx={{ 
                display: 'flex', 
                flexDirection: { xs: 'column', sm: 'row' },
                justifyContent: 'space-between', 
                alignItems: { xs: 'stretch', sm: 'center' }, 
                mb: 2,
                gap: { xs: 1, sm: 0 }
              }}>
                <Typography variant="h6" fontWeight={600}>
                  Calendar View
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                  <IconButton 
                    onClick={() => navigateMonth('prev')}
                    size={isMobile ? "small" : "medium"}
                  >
                    <NavigateBefore />
                  </IconButton>
                  <ResponsiveButton
                    variant="outlined"
                    startIcon={<Today />}
                    onClick={() => setCurrentDate(new Date())}
                    size={isMobile ? "small" : "medium"}
                  >
                    {isMobile ? 'Today' : 'Today'}
                  </ResponsiveButton>
                  <IconButton 
                    onClick={() => navigateMonth('next')}
                    size={isMobile ? "small" : "medium"}
                  >
                    <NavigateNext />
                  </IconButton>
                </Box>
              </Box>

              <Typography variant={isMobile ? "h6" : "h5"} fontWeight={600} sx={{ mb: 2 }}>
                {currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
              </Typography>

              {/* Calendar Grid */}
              <Grid container spacing={0}>
                {/* Day headers */}
                {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
                  <Grid size={12/7} key={day}>
                    <Box sx={{ 
                      p: { xs: 0.5, sm: 1 }, 
                      textAlign: 'center', 
                      fontWeight: 600,
                      bgcolor: '#f5f5f5', // Light gray background
                      border: '1px solid #e0e0e0',
                      fontSize: { xs: '0.7rem', sm: '0.875rem' },
                      color: '#333333' // Dark gray text
                    }}>
                      {day}
                    </Box>
                  </Grid>
                ))}

                {/* Calendar days */}
                {generateCalendarDays().map((date, index) => {
                  const status = getAvailabilityStatus(date);
                  return (
                    <Grid size={12/7} key={index}>
                      <CalendarDay
                        className={`
                          ${!isCurrentMonth(date) ? 'other-month' : ''}
                          ${isToday(date) ? 'today' : ''}
                          ${selectedDate?.toDateString() === date.toDateString() ? 'selected' : ''}
                          ${status}
                        `}
                        onClick={() => handleDateSelect(date)}
                        sx={{
                          opacity: isCurrentMonth(date) ? 1 : 0.3,
                          bgcolor: isToday(date) ? '#e3f2fd' : 'inherit', // Light blue for today
                          color: isToday(date) ? '#1976d2' : 'inherit', // Blue text for today
                        }}
                      >
                        <Typography variant="body2" fontWeight={500} sx={{ fontSize: { xs: '0.7rem', sm: '0.875rem' } }}>
                          {date.getDate()}
                        </Typography>
                        {status !== 'normal' && (
                          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 0.5 }}>
                            <Chip 
                              label={
                                status === 'has-slots' ? 'Available' :
                                status === 'limited' ? 'Limited' :
                                status === 'conflict' ? 'Conflict' : 'Available'
                              }
                              size="small" 
                              sx={{ 
                                fontSize: { xs: '0.5rem', sm: '0.6rem' }, 
                                height: { xs: 14, sm: 16 },
                                bgcolor: 
                                  status === 'has-slots' ? '#e8f5e8' :
                                  status === 'limited' ? '#fff3e0' :
                                  status === 'conflict' ? '#ffebee' : '#e8f5e8',
                                color: 
                                  status === 'has-slots' ? '#2e7d32' :
                                  status === 'limited' ? '#f57c00' :
                                  status === 'conflict' ? '#d32f2f' : '#2e7d32',
                                '& .MuiChip-label': {
                                  px: { xs: 0.5, sm: 1 }
                                }
                              }}
                            />
                          </Box>
                        )}
                      </CalendarDay>
                    </Grid>
                  );
                })}
              </Grid>
            </CardContent>
          </StyledCard>
        </Grid>

        {/* Quick Actions & Stats */}
        <Grid size={{ xs: 12, lg: 4 }}>
          <Grid container spacing={{ xs: 1, sm: 2 }}>
            <Grid size={12}>
              <StyledCard>
                <CardContent sx={{ p: { xs: 1.5, sm: 2 } }}>
                  <Typography variant="h6" fontWeight={600} gutterBottom>
                    Quick Actions
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    <ResponsiveButton
                      variant="outlined"
                      startIcon={<Add />}
                      onClick={() => handleOpenDialog('add')}
                      fullWidth
                      size={isMobile ? "small" : "medium"}
                    >
                      Add Single Slot
                    </ResponsiveButton>
                    <ResponsiveButton
                      variant="outlined"
                      startIcon={<Schedule />}
                      fullWidth
                      size={isMobile ? "small" : "medium"}
                      onClick={() => addNotification({ type: 'info', message: 'Batch add feature coming soon!' })}
                    >
                      Batch Add Slots
                    </ResponsiveButton>
                    <ResponsiveButton
                      variant="outlined"
                      startIcon={<Settings />}
                      fullWidth
                      size={isMobile ? "small" : "medium"}
                      onClick={() => addNotification({ type: 'info', message: 'Recurring schedule feature coming soon!' })}
                    >
                      Recurring Schedule
                    </ResponsiveButton>
                  </Box>
                </CardContent>
              </StyledCard>
            </Grid>

            <Grid size={12}>
              <StyledCard>
                <CardContent sx={{ p: { xs: 1.5, sm: 2 } }}>
                  <Typography variant="h6" fontWeight={600} gutterBottom>
                    This Month
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Available Slots
                      </Typography>
                      <Typography variant={isMobile ? "h5" : "h4"} fontWeight={700} sx={{ color: '#4caf50' }}>
                        156
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Booked Appointments
                      </Typography>
                      <Typography variant={isMobile ? "h5" : "h4"} fontWeight={700} sx={{ color: '#2196f3' }}>
                        89
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        Utilization Rate
                      </Typography>
                      <Typography variant={isMobile ? "h5" : "h4"} fontWeight={700} sx={{ color: '#ff9800' }}>
                        57%
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </StyledCard>
            </Grid>
          </Grid>
        </Grid>
      </Grid>

      {/* Availability Form Dialog */}
      <Dialog 
        open={openDialog} 
        onClose={handleCloseDialog}
        maxWidth="md"
        fullWidth
        fullScreen={isMobile}
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Schedule />
            {dialogMode === 'add' ? 'Add Availability' : 'Edit Availability'}
          </Box>
        </DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <Grid container spacing={{ xs: 2, sm: 3 }}>
              {/* Basic Details */}
              <Grid size={12}>
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Basic Details
                </Typography>
              </Grid>
              
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Provider"
                  value={formData.provider}
                  onChange={(e) => handleFormChange('provider', e.target.value)}
                  fullWidth
                  select
                  size={isMobile ? "small" : "medium"}
                >
                  <MenuItem value="Dr. Sarah Johnson">Dr. Sarah Johnson</MenuItem>
                  <MenuItem value="Dr. Michael Chen">Dr. Michael Chen</MenuItem>
                </TextField>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Date"
                  type="date"
                  value={formData.date}
                  onChange={(e) => handleFormChange('date', e.target.value)}
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Start Time"
                  type="time"
                  value={formData.startTime}
                  onChange={(e) => handleFormChange('startTime', e.target.value)}
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="End Time"
                  type="time"
                  value={formData.endTime}
                  onChange={(e) => handleFormChange('endTime', e.target.value)}
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Timezone"
                  value={formData.timezone}
                  onChange={(e) => handleFormChange('timezone', e.target.value)}
                  fullWidth
                  select
                  size={isMobile ? "small" : "medium"}
                >
                  <MenuItem value="America/New_York">America/New_York</MenuItem>
                  <MenuItem value="Europe/London">Europe/London</MenuItem>
                  <MenuItem value="Asia/Tokyo">Asia/Tokyo</MenuItem>
                </TextField>
              </Grid>

              {/* Recurring Settings */}
              <Grid size={12}>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Recurring Settings
                </Typography>
              </Grid>

              <Grid size={12}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.isRecurring}
                      onChange={(e) => handleFormChange('isRecurring', e.target.checked)}
                    />
                  }
                  label="Is Recurring?"
                />
              </Grid>

              {formData.isRecurring && (
                <>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      label="Recurrence Pattern"
                      value={formData.recurrencePattern}
                      onChange={(e) => handleFormChange('recurrencePattern', e.target.value)}
                      fullWidth
                      select
                      size={isMobile ? "small" : "medium"}
                    >
                      <MenuItem value="daily">Daily</MenuItem>
                      <MenuItem value="weekly">Weekly</MenuItem>
                      <MenuItem value="monthly">Monthly</MenuItem>
                    </TextField>
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      label="Recurrence End Date"
                      type="date"
                      value={formData.recurrenceEndDate}
                      onChange={(e) => handleFormChange('recurrenceEndDate', e.target.value)}
                      fullWidth
                      InputLabelProps={{ shrink: true }}
                      size={isMobile ? "small" : "medium"}
                    />
                  </Grid>
                </>
              )}

              {/* Slot Settings */}
              <Grid size={12}>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Slot Settings
                </Typography>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Slot Duration (minutes)"
                  type="number"
                  value={formData.slotDuration}
                  onChange={(e) => handleFormChange('slotDuration', parseInt(e.target.value))}
                  fullWidth
                  InputProps={{
                    endAdornment: <InputAdornment position="end">min</InputAdornment>,
                  }}
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Break Duration (minutes)"
                  type="number"
                  value={formData.breakDuration}
                  onChange={(e) => handleFormChange('breakDuration', parseInt(e.target.value))}
                  fullWidth
                  InputProps={{
                    endAdornment: <InputAdornment position="end">min</InputAdornment>,
                  }}
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Max Appointments Per Slot"
                  type="number"
                  value={formData.maxAppointments}
                  onChange={(e) => handleFormChange('maxAppointments', parseInt(e.target.value))}
                  fullWidth
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Appointment Type"
                  value={formData.appointmentType}
                  onChange={(e) => handleFormChange('appointmentType', e.target.value)}
                  fullWidth
                  select
                  size={isMobile ? "small" : "medium"}
                >
                  {appointmentTypes.map(type => (
                    <MenuItem key={type} value={type}>{type}</MenuItem>
                  ))}
                </TextField>
              </Grid>

              {/* Location */}
              <Grid size={12}>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Location
                </Typography>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Location Type"
                  value={formData.locationType}
                  onChange={(e) => handleFormChange('locationType', e.target.value)}
                  fullWidth
                  select
                  size={isMobile ? "small" : "medium"}
                >
                  {locationTypes.map(type => (
                    <MenuItem key={type} value={type}>{type}</MenuItem>
                  ))}
                </TextField>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Room Number"
                  value={formData.roomNumber}
                  onChange={(e) => handleFormChange('roomNumber', e.target.value)}
                  fullWidth
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              <Grid size={12}>
                <TextField
                  label="Address"
                  value={formData.address}
                  onChange={(e) => handleFormChange('address', e.target.value)}
                  fullWidth
                  multiline
                  rows={2}
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              {/* Pricing */}
              <Grid size={12}>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Pricing
                </Typography>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Base Fee"
                  type="number"
                  value={formData.baseFee}
                  onChange={(e) => handleFormChange('baseFee', e.target.value)}
                  fullWidth
                  InputProps={{
                    startAdornment: <InputAdornment position="start">$</InputAdornment>,
                  }}
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  label="Currency"
                  value={formData.currency}
                  onChange={(e) => handleFormChange('currency', e.target.value)}
                  fullWidth
                  select
                  size={isMobile ? "small" : "medium"}
                >
                  {currencies.map(currency => (
                    <MenuItem key={currency} value={currency}>{currency}</MenuItem>
                  ))}
                </TextField>
              </Grid>

              <Grid size={12}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={formData.insuranceAccepted}
                      onChange={(e) => handleFormChange('insuranceAccepted', e.target.checked)}
                    />
                  }
                  label="Insurance Accepted"
                />
              </Grid>

              {/* Additional Information */}
              <Grid size={12}>
                <Divider sx={{ my: 2 }} />
                <Typography variant="h6" fontWeight={600} gutterBottom>
                  Additional Information
                </Typography>
              </Grid>

              <Grid size={12}>
                <TextField
                  label="Notes"
                  value={formData.notes}
                  onChange={(e) => handleFormChange('notes', e.target.value)}
                  fullWidth
                  multiline
                  rows={3}
                  inputProps={{ maxLength: 500 }}
                  helperText={`${formData.notes.length}/500 characters`}
                  size={isMobile ? "small" : "medium"}
                />
              </Grid>
            </Grid>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: { xs: 2, sm: 3 } }}>
          <Button 
            onClick={handleCloseDialog}
            disabled={loading}
            size={isMobile ? "small" : "medium"}
          >
            Cancel
          </Button>
          <Button 
            variant="contained" 
            onClick={handleSave}
            startIcon={loading ? <CircularProgress size={16} /> : <Save />}
            disabled={loading}
            size={isMobile ? "small" : "medium"}
          >
            {loading ? 'Saving...' : 'Save Availability'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Mobile Speed Dial */}
      {isMobile && (
        <>
          <Fab
            color="primary"
            aria-label="add"
            sx={{ position: 'fixed', bottom: 16, right: 16 }}
            onClick={() => setOpenSpeedDial(true)}
          >
            <Add />
          </Fab>
          <Backdrop open={openSpeedDial} sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }} />
          <SpeedDial
            ariaLabel="Availability actions"
            sx={{ position: 'fixed', bottom: 16, right: 16 }}
            icon={<SpeedDialIcon />}
            onClose={() => setOpenSpeedDial(false)}
            onOpen={() => setOpenSpeedDial(true)}
            open={openSpeedDial}
          >
            {speedDialActions.map((action) => (
              <SpeedDialAction
                key={action.name}
                icon={action.icon}
                tooltipTitle={action.name}
                onClick={() => {
                  action.action();
                  setOpenSpeedDial(false);
                }}
              />
            ))}
          </SpeedDial>
        </>
      )}

      {/* Notification System */}
      {notifications.map((notification) => (
        <Snackbar
          key={notification.id}
          open={true}
          autoHideDuration={6000}
          onClose={() => removeNotification(notification.id)}
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
          sx={{ mt: 8 }}
        >
          <Alert
            onClose={() => removeNotification(notification.id)}
            severity={getStatusColor(notification.type)}
            icon={getStatusIcon(notification.type)}
            sx={{ width: '100%' }}
            action={
              <IconButton
                color="inherit"
                size="small"
                onClick={() => removeNotification(notification.id)}
              >
                <Close fontSize="inherit" />
              </IconButton>
            }
          >
            <Box>
              {notification.title && (
                <Typography variant="subtitle2" fontWeight={600}>
                  {notification.title}
                </Typography>
              )}
              <Typography variant="body2">
                {notification.message}
              </Typography>
            </Box>
          </Alert>
        </Snackbar>
      ))}
    </Box>
  );
};

export default ProviderAvailability; 