package com.flooat.catbox;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flooat.catbox.models.Message;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<Message> mMessages;
    private int[] mNameColors;

    public MessageAdapter(Context context, List<Message> messages) {
        mMessages = messages;
        mNameColors = context.getResources().getIntArray(R.array.username_colors);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = -1;
        switch (viewType) {
            case Message.TYPE_MESSAGE:
                layout = R.layout.item_message;
                break;
            case Message.TYPE_LOG:
                layout = R.layout.item_log;
                break;
            case Message.TYPE_ACTION:
                layout = R.layout.item_action;
                break;
        }
        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Message message = mMessages.get(position);
        viewHolder.setMessage(message.getMessage());
        viewHolder.setName(message.getName());
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mMessages.get(position).getType();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mNameView;
        private TextView mMessageView;

        public ViewHolder(View itemView) {
            super(itemView);

            mNameView = (TextView) itemView.findViewById(R.id.name);
            mMessageView = (TextView) itemView.findViewById(R.id.message);
        }

        public void setName(String name) {
            if (null == mNameView) return;
            mNameView.setText(name);
            mNameView.setTextColor(getNameColor(name));
        }

        public void setMessage(String message) {
            if (null == mMessageView) return;
            mMessageView.setText(message);
        }

        /*
         * Convert name string to hash and get a matching color
         */
        private int getNameColor(String name) {
            int hash = 7;
            for (int i = 0, len = name.length(); i < len; i++) {
                hash = name.codePointAt(i) + (hash << 5) - hash;
            }
            int index = Math.abs(hash % mNameColors.length);
            return mNameColors[index];
        }
    }
}
