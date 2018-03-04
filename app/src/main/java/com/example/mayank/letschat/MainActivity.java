package com.example.mayank.letschat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ortiz.touchview.TouchImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MessagesAdapter.ListItemClickListener{

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH = 1000;
    public static final int RC_SIGN_IN = 1;
    public static final int RC_PHOTO_PICKER = 2;

    private RecyclerView mMessageListView;
    private MessagesAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageView mPhotoPickerButton;
    private EditText mMessageEditText;
    private ImageView mSendButton;
    private TouchImageView mLargeImage;
    private RelativeLayout mRelativeLayout;
    ArrayList<ChatMessage> mChatMessages;
    public LinearLayoutManager mLayoutManager;
    private static String mUsername;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        //Initialize views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (RecyclerView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageView) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (ImageView) findViewById(R.id.sendButton);
        mLargeImage = (TouchImageView)findViewById(R.id.largeImage);
        mRelativeLayout = (RelativeLayout)findViewById(R.id.relativeLayout);

        //Initialiaze Recycler View and Adapters
        mChatMessages = new ArrayList<>();
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);
        mMessageListView.setLayoutManager(mLayoutManager);
        mMessageAdapter  = new MessagesAdapter(mChatMessages,this);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/jpeg");
                i.putExtra(i.EXTRA_LOCAL_ONLY,true);
                startActivityForResult(Intent.createChooser(i,"Complete action using : "),RC_PHOTO_PICKER);
            }
        });

        //Initialize Firebase instances
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //mFirebaseDatabase.setPersistenceEnabled(true);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0 && !charSequence.toString().equals("")) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!(mMessageEditText.getText().toString().equals("") || mMessageEditText.getText().toString().isEmpty()))
                {
                    ChatMessage chatMessage = new ChatMessage(mMessageEditText.getText().toString(), mUsername, null);
                    mMessagesDatabaseReference.push().setValue(chatMessage);
                }
                // Clear input box
                mMessageEditText.setText("");
            }
        });
        if(mAuthStateListener==null)
            mAuthStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = mFirebaseAuth.getCurrentUser();
                    final List<AuthUI.IdpConfig> signInMethods = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            //new AuthUI.IdpConfig.PhoneBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build());
                    if (user != null) {
                        String username = user.getDisplayName();
                        onSignedInInitialize(username);
                    } else {
                        onSignedOutCleanup();
                        startActivityForResult(
                                AuthUI.getInstance()
                                        .createSignInIntentBuilder()
                                        .setIsSmartLockEnabled(false)
                                        .setAvailableProviders(signInMethods)
                                        .setTheme(R.style.LoginTheme)
                                        //.setLogo(0)
                                        .build(),
                                RC_SIGN_IN);
                    }
                }
            };

    }

    @Override
    protected void onResume() {
        super.onResume();
        mMessageAdapter.swapArrayList(mChatMessages);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthStateListener != null)
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        detachReadListener();
        mChatMessages.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN)
        {
            if(resultCode == RESULT_OK)
            {
                Toast.makeText(this,"Signed in !",Toast.LENGTH_SHORT).show();
            }
            else if(resultCode == RESULT_CANCELED)
            {
                Toast.makeText(this,"Sign in cancelled",Toast.LENGTH_SHORT).show();
                finish();
            }
        }else if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK)
        {
            Uri selectedImageUri = data.getData();
            StorageReference reference = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
            reference.putFile(selectedImageUri).addOnSuccessListener(this,new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    ChatMessage chatMessage = new ChatMessage(null,mUsername,downloadUrl.toString());
                    mMessagesDatabaseReference.push().setValue(chatMessage);
                }
            });

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.sign_out_menu)
        {
            AuthUI.getInstance().signOut(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(String imageRes) {

        Glide.with(mLargeImage.getContext()).load(imageRes).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                mLargeImage.setImageBitmap(resource);
                triggerLargeImage(1);
            }
        });
    }
    @Override
    public void onBackPressed() {
        if(mLargeImage.getVisibility() == View.GONE)
            super.onBackPressed();
        else {triggerLargeImage(0);}
    }



    private void onSignedInInitialize(String username)
    {
        mUsername = username;
        mMessageAdapter.changeUserName();
        //mMessageAdapter.swapArrayList(new ArrayList<ChatMessage>(0));
        attachReadListener();
    }

    private void attachReadListener()
    {
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                    mChatMessages.add(message);
                    {
                        mMessageAdapter.swapArrayList(mChatMessages);
                        mMessageListView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
                        Log.v(TAG,mChatMessages.size()+"");
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }
    private void onSignedOutCleanup()
    {
        mUsername = ANONYMOUS;
        //mMessageAdapter.swapArrayList(new ArrayList<ChatMessage>(0));
        detachReadListener();
        mChatMessages.clear();
    }
    private void detachReadListener()
    {
        if(mChildEventListener != null)
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
        mChildEventListener =null;
    }
    public void triggerLargeImage(int type)
    {
        if(type == 1) {
            mLargeImage.setVisibility(View.VISIBLE);
            mRelativeLayout.setVisibility(View.INVISIBLE);
        }else {
            mLargeImage.setVisibility(View.GONE);
            mRelativeLayout.setVisibility(View.VISIBLE);
        }
    }

}
