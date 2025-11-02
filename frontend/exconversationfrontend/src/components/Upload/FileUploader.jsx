import { useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { toast } from 'react-toastify';
import './FileUploader.css';

function FileUploader({ onUpload }) {
  const [uploading, setUploading] = useState(false);

  const onDrop = async (acceptedFiles) => {
    if (acceptedFiles.length === 0) {
      toast.warning('Please select a .docx file');
      return;
    }

    const file = acceptedFiles[0];
    if (!file.name.toLowerCase().endsWith('.docx')) {
      toast.error('Only .docx files are supported');
      return;
    }

    try {
      setUploading(true);
      await onUpload(file, 'System');
    } catch (error) {
      // Error already handled in parent
    } finally {
      setUploading(false);
    }
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
    },
    multiple: false,
  });

  return (
    <div className="file-uploader">
      <div
        {...getRootProps()}
        className={`dropzone ${isDragActive ? 'active' : ''} ${uploading ? 'uploading' : ''}`}
      >
        <input {...getInputProps()} />
        {uploading ? (
          <div className="upload-status">
            <div className="spinner"></div>
            <p>Uploading...</p>
          </div>
        ) : (
          <div className="upload-content">
            <div className="upload-icon">📄</div>
            <p className="upload-text">
              {isDragActive
                ? 'Drop the file here...'
                : 'Drag & drop a DOCX file here, or click to select'}
            </p>
            <p className="upload-hint">Only .docx files are accepted</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default FileUploader;

