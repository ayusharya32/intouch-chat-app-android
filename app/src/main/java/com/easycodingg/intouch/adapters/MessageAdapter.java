package com.easycodingg.intouch.adapters;

import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.easycodingg.intouch.databinding.MessageItemLeftBinding;
import com.easycodingg.intouch.databinding.MessageItemRightBinding;
import com.easycodingg.intouch.models.Message;
import com.easycodingg.intouch.utils.CommonMethods;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.diffutil.MessageDiffUtil;
import com.easycodingg.intouch.utils.events.LeftMessageItemClickEvent;
import com.easycodingg.intouch.utils.events.RightMessageItemClickEvent;

import java.util.Calendar;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class MessageAdapter extends ListAdapter<Message, MessageAdapter.MessageViewHolder> {
    private static final String TAG = "MessageAdapteryyyy";

    private static final int MESSAGE_LEFT = 1;
    private static final int MESSAGE_RIGHT = 2;

    private LeftMessageItemClickEvent leftClickEvent;
    private RightMessageItemClickEvent rightClickEvent;

    public MessageAdapter(LeftMessageItemClickEvent leftClickEvent,
                          RightMessageItemClickEvent rightClickEvent) {
        super(new MessageDiffUtil());

        this.leftClickEvent = leftClickEvent;
        this.rightClickEvent = rightClickEvent;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        if(viewType == MESSAGE_LEFT) {
            return new MessageViewHolder(MessageItemLeftBinding.inflate(layoutInflater,
                    parent, false));
        } else {
            return new MessageViewHolder(MessageItemRightBinding.inflate(layoutInflater,
                    parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = getItem(position);
        holder.bind(message);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = getItem(position);

        if(message.sentByUserId.equals(CommonVariables.loggedInUser.id)) {
            return MESSAGE_RIGHT;
        }

        return MESSAGE_LEFT;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        private MessageItemLeftBinding leftItemBinding;
        private MessageItemRightBinding rightItemBinding;

        private Message message;

        public MessageViewHolder(MessageItemLeftBinding binding) {
            super(binding.getRoot());
            this.leftItemBinding = binding;

            setupLeftMessageItemClickListeners();
        }

        public MessageViewHolder(MessageItemRightBinding binding) {
            super(binding.getRoot());
            this.rightItemBinding = binding;

            setupRightMessageItemClickListeners();
        }

        public void bind(Message message) {
            this.message = message;

            if(message.orderTimestamp == null) {
                message.orderTimestamp = message.sendingTime;
            }

            if(getItemViewType() == MESSAGE_LEFT) {
                setupLeftMessageItem();
            } else {
                setupRightMessageItem();
            }
        }

        private void setupLeftMessageItem() {
            switch(message.type) {
                case TYPE_TEXT:
                    setupLeftTextMessage();
                    break;

                case TYPE_IMAGE:
                    setupLeftImageMessage();
                    break;

                case TYPE_DOCUMENT:
                    setupLeftDocumentMessage();
                    break;
            }

            leftItemBinding.txtMessageTime.setText(CommonMethods.getFormattedTime(message.orderTimestamp));

            Log.d(TAG, getBindingAdapterPosition() + "");

            if(getBindingAdapterPosition() == 0) {
                Log.d(TAG, "First Position");
                showDateInLeftMessage();

            } else {
                checkPreviousMessageAndShowDate(message);
            }
        }

        private void setupRightMessageItem() {
            switch(message.type) {
                case TYPE_TEXT:
                    setupRightTextMessage();
                    break;

                case TYPE_IMAGE:
                    setupRightImageMessage();
                    break;

                case TYPE_DOCUMENT:
                    setupRightDocumentMessage();
                    break;
            }

            rightItemBinding.txtMessageTime.setText(CommonMethods.getFormattedTime(message.orderTimestamp));

            Log.d(TAG, getBindingAdapterPosition() + "");

            if(getBindingAdapterPosition() == 0) {
                Log.d(TAG, "First Position");
                showDateInRightMessage();

            } else {
                checkPreviousMessageAndShowDate(message);
            }

            rightItemBinding.imgMessageStatus.setImageResource(CommonMethods.getMessageStatusImageId(message.status));
            rightItemBinding.imgMessageStatus.setImageTintList(ColorStateList.valueOf(
                    CommonMethods.getMessageStatusImageColor(itemView.getContext(), message.status)));
        }

        public void checkPreviousMessageAndShowDate(Message currentMessage) {
            Message previousMessage = getItem(getBindingAdapterPosition() - 1);

            Calendar calPreviousMessage = Calendar.getInstance();
            calPreviousMessage.setTime(previousMessage.sendingTime);

            Calendar calCurrentMessage = Calendar.getInstance();
            calCurrentMessage.setTime(currentMessage.sendingTime);

            boolean sameDay = calPreviousMessage.get(Calendar.DAY_OF_YEAR) ==
                    calCurrentMessage.get(Calendar.DAY_OF_YEAR) &&
                    calPreviousMessage.get(Calendar.YEAR) ==
                            calCurrentMessage.get(Calendar.YEAR);

            Log.d(TAG, "Previous: " + calPreviousMessage.get(Calendar.DAY_OF_YEAR) + " " + calPreviousMessage.get(Calendar.YEAR));
            Log.d(TAG, "Current: " + calCurrentMessage.get(Calendar.DAY_OF_YEAR) + " " + calCurrentMessage.get(Calendar.YEAR));
            Log.d(TAG, "Same Day: " + sameDay + "  " + getBindingAdapterPosition());

            handleDateShow(sameDay);
        }

        private void handleDateShow(boolean sameDay) {
            if(getItemViewType() == MESSAGE_LEFT) {
                if(!sameDay) {
                    showDateInLeftMessage();
                } else {
                    leftItemBinding.txtDate.setVisibility(View.GONE);
                }

            } else {
                if(!sameDay) {
                    showDateInRightMessage();
                } else {
                    rightItemBinding.txtDate.setVisibility(View.GONE);
                }
            }
        }

        private void showDateInLeftMessage() {
            leftItemBinding.txtDate.setText(CommonMethods.getFormattedDate(message.orderTimestamp));
            leftItemBinding.txtDate.setVisibility(View.VISIBLE);
        }

        private void showDateInRightMessage() {
            rightItemBinding.txtDate.setText(CommonMethods.getFormattedDate(message.orderTimestamp));
            rightItemBinding.txtDate.setVisibility(View.VISIBLE);
        }

        private void setupLeftTextMessage() {
            leftItemBinding.txtMessage.setText(message.content);
            leftItemBinding.txtMessage.setVisibility(View.VISIBLE);
            leftItemBinding.flImageMessage.setVisibility(View.GONE);
            leftItemBinding.llDocument.setVisibility(View.GONE);
        }

        private void setupLeftImageMessage() {
            if(!message.content.isEmpty()) {
                leftItemBinding.txtMessage.setText(message.content);
                leftItemBinding.txtMessage.setVisibility(View.VISIBLE);

            } else {
                leftItemBinding.txtMessage.setVisibility(View.GONE);
            }

            if(message.localFileUriString != null) {
                Glide.with(leftItemBinding.getRoot())
                        .load(message.localFileUriString)
                        .into(leftItemBinding.imgMessage);

                leftItemBinding.btnDownloadImageMessage.setVisibility(View.GONE);

            } else {
                Glide.with(leftItemBinding.getRoot())
                        .load(message.fileDownloadUrl)
                        .apply(RequestOptions.bitmapTransform(new BlurTransformation(40)))
                        .into(leftItemBinding.imgMessage);

                leftItemBinding.btnDownloadImageMessage.setVisibility(View.VISIBLE);
            }

            Log.d(TAG, "setupLeftImageMessage: fileDownloadStatus " + message.fileDownloadStatus);
            if(message.fileDownloadStatus != null) {
                switch(message.fileDownloadStatus) {
                    case NOT_DOWNLOADING:
                        leftItemBinding.btnStopImageDownload.setVisibility(View.GONE);
                        leftItemBinding.btnDownloadImageMessage.setVisibility(View.VISIBLE);
                        leftItemBinding.progressIndicatorImageMessage.setVisibility(View.GONE);
                        break;

                    case DOWNLOADING:
                        leftItemBinding.btnStopImageDownload.setVisibility(View.VISIBLE);
                        leftItemBinding.btnDownloadImageMessage.setVisibility(View.GONE);

                        leftItemBinding.progressIndicatorDocumentMessage.setIndeterminate(false);
                        leftItemBinding.progressIndicatorImageMessage.setProgress(message.downloadingPercentage);
                        leftItemBinding.progressIndicatorImageMessage.setVisibility(View.VISIBLE);
                        break;

                    case DOWNLOADED:
                        leftItemBinding.btnStopImageDownload.setVisibility(View.GONE);
                        leftItemBinding.btnDownloadImageMessage.setVisibility(View.GONE);
                        leftItemBinding.progressIndicatorImageMessage.setVisibility(View.GONE);
                        break;
                }
            }

            leftItemBinding.flImageMessage.setVisibility(View.VISIBLE);
            leftItemBinding.imgMessage.setVisibility(View.VISIBLE);
            leftItemBinding.llDocument.setVisibility(View.GONE);
        }

        private void setupLeftDocumentMessage() {
            leftItemBinding.txtDocumentName.setText(message.content);
            leftItemBinding.txtMessage.setVisibility(View.GONE);

            if(message.localFileUriString != null) {
                leftItemBinding.btnDownloadDocumentMessage.setVisibility(View.GONE);

            } else {
                leftItemBinding.btnDownloadDocumentMessage.setVisibility(View.VISIBLE);
            }

            if(message.fileDownloadStatus != null) {
                switch(message.fileDownloadStatus) {
                    case NOT_DOWNLOADING:
                        leftItemBinding.btnStopDocumentDownload.setVisibility(View.GONE);
                        leftItemBinding.btnDownloadDocumentMessage.setVisibility(View.VISIBLE);
                        leftItemBinding.progressIndicatorDocumentMessage.setVisibility(View.GONE);
                        break;

                    case DOWNLOADING:
                        leftItemBinding.btnStopDocumentDownload.setVisibility(View.VISIBLE);
                        leftItemBinding.btnDownloadDocumentMessage.setVisibility(View.GONE);

                        leftItemBinding.progressIndicatorDocumentMessage.setIndeterminate(false);
                        leftItemBinding.progressIndicatorDocumentMessage.setProgress(message.downloadingPercentage);
                        leftItemBinding.progressIndicatorDocumentMessage.setVisibility(View.VISIBLE);
                        break;

                    case DOWNLOADED:
                        leftItemBinding.btnStopDocumentDownload.setVisibility(View.GONE);
                        leftItemBinding.btnDownloadDocumentMessage.setVisibility(View.GONE);
                        leftItemBinding.progressIndicatorDocumentMessage.setVisibility(View.GONE);
                        break;
                }
            }

            leftItemBinding.llDocument.setVisibility(View.VISIBLE);
            leftItemBinding.flImageMessage.setVisibility(View.GONE);
        }

        private void setupRightTextMessage() {
            rightItemBinding.txtMessage.setText(message.content);
            rightItemBinding.txtMessage.setVisibility(View.VISIBLE);
            rightItemBinding.flImageMessage.setVisibility(View.GONE);
            rightItemBinding.llDocument.setVisibility(View.GONE);
        }

        private void setupRightImageMessage() {
            if(!message.content.isEmpty()) {
                rightItemBinding.txtMessage.setText(message.content);
                rightItemBinding.txtMessage.setVisibility(View.VISIBLE);

            } else {
                rightItemBinding.txtMessage.setVisibility(View.GONE);
            }

            Glide.with(rightItemBinding.getRoot())
                    .load(message.localFileUriString)
                    .into(rightItemBinding.imgMessage);

            if(message.fileUploadStatus != null) {
                switch(message.fileUploadStatus) {
                    case PREPARING:
                        rightItemBinding.btnUploadImageMessage.setVisibility(View.GONE);
                        rightItemBinding.btnStopImageUpload.setVisibility(View.VISIBLE);
                        rightItemBinding.progressIndicatorImageMessage.setVisibility(View.VISIBLE);
                        rightItemBinding.progressIndicatorImageMessage.setIndeterminate(true);
                        break;

                    case NOT_UPLOADING:
                        rightItemBinding.btnUploadImageMessage.setVisibility(View.VISIBLE);
                        rightItemBinding.btnStopImageUpload.setVisibility(View.GONE);
                        rightItemBinding.progressIndicatorImageMessage.setVisibility(View.GONE);
                        break;

                    case UPLOADING:
                        rightItemBinding.btnStopImageUpload.setVisibility(View.VISIBLE);
                        rightItemBinding.btnUploadImageMessage.setVisibility(View.GONE);

                        rightItemBinding.progressIndicatorImageMessage.setIndeterminate(false);
                        rightItemBinding.progressIndicatorImageMessage.setProgress(message.uploadingPercentage);
                        rightItemBinding.progressIndicatorImageMessage.setVisibility(View.VISIBLE);
                        break;

                    case UPLOADED:
                        rightItemBinding.btnUploadImageMessage.setVisibility(View.GONE);
                        rightItemBinding.btnStopImageUpload.setVisibility(View.GONE);
                        rightItemBinding.progressIndicatorImageMessage.setVisibility(View.GONE);
                        break;
                }
            }

            rightItemBinding.flImageMessage.setVisibility(View.VISIBLE);
            rightItemBinding.imgMessage.setVisibility(View.VISIBLE);
            rightItemBinding.llDocument.setVisibility(View.GONE);
        }

        private void setupRightDocumentMessage() {
            rightItemBinding.txtDocumentName.setText(message.content);
            rightItemBinding.txtMessage.setVisibility(View.GONE);

            switch(message.fileUploadStatus) {
                case PREPARING:
                    rightItemBinding.btnUploadDocumentMessage.setVisibility(View.GONE);
                    rightItemBinding.btnStopDocumentUpload.setVisibility(View.VISIBLE);
                    rightItemBinding.progressIndicatorDocumentMessage.setVisibility(View.VISIBLE);
                    rightItemBinding.progressIndicatorDocumentMessage.setIndeterminate(true);
                    break;

                case NOT_UPLOADING:
                    rightItemBinding.btnUploadDocumentMessage.setVisibility(View.VISIBLE);
                    rightItemBinding.btnStopDocumentUpload.setVisibility(View.GONE);
                    rightItemBinding.progressIndicatorDocumentMessage.setVisibility(View.GONE);
                    break;

                case UPLOADING:
                    rightItemBinding.btnUploadDocumentMessage.setVisibility(View.GONE);
                    rightItemBinding.btnStopDocumentUpload.setVisibility(View.VISIBLE);

                    rightItemBinding.progressIndicatorDocumentMessage.setIndeterminate(false);
                    rightItemBinding.progressIndicatorDocumentMessage.setProgress(message.uploadingPercentage);
                    rightItemBinding.progressIndicatorDocumentMessage.setVisibility(View.VISIBLE);
                    break;

                case UPLOADED:
                    rightItemBinding.btnUploadDocumentMessage.setVisibility(View.GONE);
                    rightItemBinding.btnStopDocumentUpload.setVisibility(View.GONE);
                    rightItemBinding.progressIndicatorDocumentMessage.setVisibility(View.GONE);
                    break;
            }

            rightItemBinding.llDocument.setVisibility(View.VISIBLE);
            rightItemBinding.flImageMessage.setVisibility(View.GONE);
        }

        private void setupLeftMessageItemClickListeners() {
            leftItemBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(leftClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        leftClickEvent.onItemClick(message);
                    }
                }
            });

            leftItemBinding.btnDownloadImageMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(leftClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        leftClickEvent.onDownloadButtonClick(message);
                    }
                }
            });

            leftItemBinding.btnDownloadDocumentMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(leftClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        leftClickEvent.onDownloadButtonClick(message);
                    }
                }
            });

            leftItemBinding.btnStopImageDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(leftClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        leftClickEvent.onDownloadCancelButtonClick(message);
                    }
                }
            });

            leftItemBinding.btnStopDocumentDownload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(leftClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        leftClickEvent.onDownloadCancelButtonClick(message);
                    }
                }
            });
        }

        private void setupRightMessageItemClickListeners() {
            rightItemBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rightClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        rightClickEvent.onItemClick(message);
                    }
                }
            });

            rightItemBinding.btnUploadImageMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rightClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        rightClickEvent.onUploadButtonClick(message);
                    }
                }
            });

            rightItemBinding.btnUploadDocumentMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rightClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        rightClickEvent.onUploadButtonClick(message);
                    }
                }
            });

            rightItemBinding.btnStopImageUpload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rightClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        rightClickEvent.onUploadCancelButtonClick(message);
                    }
                }
            });

            rightItemBinding.btnStopDocumentUpload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rightClickEvent != null) {
                        Message message = getItem(getBindingAdapterPosition());
                        rightClickEvent.onUploadCancelButtonClick(message);
                    }
                }
            });
        }
    }
}
