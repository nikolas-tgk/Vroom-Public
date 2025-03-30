package com.p17142.vroom.ui.application.trips;

import static com.p17142.vroom.utilities.Constants.KEY_USERNAME;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.p17142.vroom.R;
import com.p17142.vroom.data.dao.TripDao;
import com.p17142.vroom.databinding.FragmentNewTripBinding;
import com.p17142.vroom.models.Trip;
import com.p17142.vroom.utilities.FragUtils;
import com.p17142.vroom.utilities.Logger;
import com.p17142.vroom.utilities.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class NewTripFragment extends Fragment {

    private PreferenceManager preferenceManager;
    private FragmentNewTripBinding binding;

    private static final String PRESELECTED_TIME = "08:00";
    private String selectedTime = PRESELECTED_TIME;

    private Date selectedTypeDate;
    private String[] cities, numOfRiders;
    private final TripDao tripDao = TripDao.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNewTripBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        setListeners();
    }

    @SuppressLint("SetTextI18n")
    private void init(){

        preferenceManager = new PreferenceManager(requireContext());

        binding.tripStartLayout.setHelperTextEnabled(false);
        binding.tripEndLayout.setHelperTextEnabled(false);
        binding.dateLayout.setHelperTextEnabled(false);
        binding.timeLayout.setHelperTextEnabled(false);
        binding.ridersLayout.setHelperTextEnabled(false);

        cities = getResources().getStringArray(R.array.cities_eng);

        ArrayAdapter<String> citiesFromAdapter = new ArrayAdapter<>(requireContext(),R.layout.item_dropdown_cities,cities);
        binding.tripStartAutoCompleteTextView.setAdapter(citiesFromAdapter);

        // adapter needs to be different or they will share the same autocomplete results
        ArrayAdapter<String> citiesToAdapter = new ArrayAdapter<>(requireContext(),R.layout.item_dropdown_cities,cities);
        binding.tripEndAutoCompleteTextView.setAdapter(citiesToAdapter);

        numOfRiders = getResources().getStringArray(R.array.num_of_riders);
        ArrayAdapter<String> numOfRidersAdapter = new ArrayAdapter<>(requireContext(),R.layout.item_dropdown_num_of_riders,numOfRiders);
        binding.ridersAutoCompleteTextView.setAdapter(numOfRidersAdapter);

        binding.timeInputText.setText(selectedTime+" AM"); // hardcoded temporary
        selectedTypeDate = new Date(getTomorrowInMillis());

        isLoading(false);
    }


    private void uploadToFirestore(){
        isLoading(true);

        Trip trip = new Trip();
        trip.setDriverUsername(preferenceManager.getString(KEY_USERNAME));
        trip.setStartLocation(binding.tripStartAutoCompleteTextView.getText().toString().trim());
        trip.setEndLocation(binding.tripEndAutoCompleteTextView.getText().toString().trim());
        trip.setStartTime(selectedTime);
        trip.setTripDate(selectedTypeDate);
        trip.setMaxNumOfRiders(Integer.parseInt(String.valueOf(binding.ridersAutoCompleteTextView.getText())));
        trip.setRiderUsernames(new ArrayList<>());
        trip.setInvitedUsernames(new ArrayList<>());
        trip.setDateCreated(new Date());

        tripDao.putNewTrip(trip)
                .addOnSuccessListener( task -> {
                    isLoading(false);
                    Toast.makeText(requireContext(),"Trip successfully published!",Toast.LENGTH_SHORT).show();
                    popBackStackAndGoHome();
                })
                .addOnFailureListener( e -> {
                   isLoading(false);
                   Logger.printLogFatal(NewTripFragment.class,"Unexpected Error on putNewTrip with error: "+e);
                });
    }

    // returns millis value of the next calendar day from today
    private long getTomorrowInMillis(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        return calendar.getTimeInMillis();
    }

    private boolean validateAllData(String fromCity, String destinationCity){
        // date and time are already registered
        boolean fromCityValid = false;
        boolean destinationCityValid = false;

        if(fromCity.isEmpty() && destinationCity.isEmpty()){
            binding.tripStartLayout.setError("Select a city from the dropdown box");
            binding.tripStartLayout.setErrorEnabled(true);

            binding.tripEndLayout.setError("Select a city from the dropdown box");
            binding.tripEndLayout.setErrorEnabled(true);
            return false;
        }
        else if(fromCity.isEmpty()){
            binding.tripStartLayout.setError("Select a city from the dropdown box");
            binding.tripStartLayout.setErrorEnabled(true);
            return false;
        } else if(destinationCity.isEmpty())
        {
            binding.tripEndLayout.setError("Select a city from the dropdown box");
            binding.tripEndLayout.setErrorEnabled(true);
            return false;
        }

        for (String city: cities
        ) {
            if(city.equals(fromCity))
            {
                fromCityValid = true;
            }
            if(city.equals(destinationCity))
            {
                destinationCityValid = true;
            }
        }
        if(!fromCityValid && !destinationCityValid){
            binding.tripStartLayout.setError("Invalid city");
            binding.tripStartLayout.setErrorEnabled(true);

            binding.tripEndLayout.setError("Invalid city");
            binding.tripEndLayout.setErrorEnabled(true);
            return false;
        }
        else if(!fromCityValid){
            binding.tripStartLayout.setError("Invalid city");
            binding.tripStartLayout.setErrorEnabled(true);
            return false;
        } else if(!destinationCityValid)
        {
            binding.tripEndLayout.setError("Invalid city");
            binding.tripEndLayout.setErrorEnabled(true);
            return false;
        }
        if(fromCity.equals(destinationCity))
        {
            binding.tripEndLayout.setError("You must select a different city");
            binding.tripEndLayout.setErrorEnabled(true);
            return false;
        }
        return true;
    }

    @SuppressLint("DefaultLocale")
    private void setListeners(){
        binding.backButton.setOnClickListener( click -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.tripStartAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length()==0)
                {
                    binding.tripStartLayout.setError("Depart city can't be empty!");
                }
                else{
                    binding.tripStartLayout.setErrorEnabled(false);
                    binding.tripStartLayout.setHelperTextEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        binding.tripEndAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length()==0)
                {
                    binding.tripEndLayout.setError("Destination city can't be empty!");
                }
                else{
                    binding.tripEndLayout.setErrorEnabled(false);
                    binding.tripEndLayout.setHelperTextEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // time picker setup
        binding.timeInputText.setOnClickListener( (unused) ->{
            binding.timeInputText.setEnabled(false);
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(0)
                    .setMinute(0)
                    .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                    .setTitleText("At what time will the trip begin?")
                    .build();

            timePicker.addOnPositiveButtonClickListener( (view) -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                selectedTime = String.format("%02d:%02d", hour, minute);
                binding.timeInputText.setText(selectedTime);
                binding.timeInputText.setEnabled(true);
            });
            timePicker.show(requireActivity().getSupportFragmentManager(), "tag");

            timePicker.addOnCancelListener( unused2 -> binding.timeInputText.setEnabled(true));
            timePicker.addOnDismissListener( unused2 -> binding.timeInputText.setEnabled(true));

        });

        // date picker setup
        binding.dateInputText.setOnClickListener( unused -> {
            binding.dateInputText.setEnabled(false); // disable so to not multi click/call OnClickListeners, will re-enable on finish/cancel
            CalendarConstraints.Builder calendarConstraintsBuilder = new CalendarConstraints.Builder();
            long tomorrowInMillis = getTomorrowInMillis();
            //long todayInMillis = Calendar.getInstance().getTimeInMillis();

            calendarConstraintsBuilder.setStart(Calendar.getInstance().getTimeInMillis()); // setting up "no previous dates" constraint to apply later on datePicker builder.
            CalendarConstraints.DateValidator dateValidator = DateValidatorPointForward.from(Calendar.getInstance().getTimeInMillis());
            calendarConstraintsBuilder.setValidator(dateValidator);

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(tomorrowInMillis) // pre-selected date
                    .setCalendarConstraints(calendarConstraintsBuilder.build()) // apply constraint (no previous dates)
                    .build();
            datePicker.addOnPositiveButtonClickListener( (selection -> {
                Date selectedDate = new Date(selection); // selected date includes full date and time
                selectedTypeDate = selectedDate;
                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM"); // format date to e.g. 14 Jan
                binding.dateInputText.setText(dateFormat.format(selectedDate)); // print formated date
                binding.dateInputText.setEnabled(true); // re-enable positive/normal case
            }));
            datePicker.show(requireActivity().getSupportFragmentManager(), "tag");
            datePicker.addOnCancelListener( unused2 -> binding.dateInputText.setEnabled(true)); // re-enable datePicker on cancel
            datePicker.addOnDismissListener( unused2 -> binding.dateInputText.setEnabled(true)); // on dismiss
        });

        binding.publishButton.setOnClickListener( (unused)->{
            isLoading(true);
            String fromCity = binding.tripStartAutoCompleteTextView.getText().toString().trim();
            String destinationCity = binding.tripEndAutoCompleteTextView.getText().toString().trim();
            if(validateAllData(fromCity,destinationCity))
            {
                uploadToFirestore();
            }
            else{
                isLoading(false);
            }
        });
    }

    private void isLoading(Boolean isLoading){
        if(isLoading){
            binding.publishButton.setClickable(false);
            binding.publishText.setVisibility(View.INVISIBLE);
            binding.publishLoadingBar.setVisibility(View.VISIBLE);
        }else{
            binding.publishButton.setClickable(true);
            binding.publishLoadingBar.setVisibility(View.INVISIBLE);
            binding.publishText.setVisibility(View.VISIBLE);
        }
    }

    private void popBackStackAndGoHome(){
        requireActivity().getSupportFragmentManager().popBackStack();
        TripHomeFragment fragment = new TripHomeFragment();
        FragUtils.replaceFragment(getParentFragmentManager(), fragment, null);
    }
}