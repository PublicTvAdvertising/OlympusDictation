#include "stdafx_ff.h"

#include <vector>
#include <string>
#include <stdexcept>
#include <algorithm>
#include <cctype>
#include <fstream>
#include <iostream>
#include <sstream>
#include <ctime>
#include <memory>
#include<math.h>
#include "DssElement.h"
#include "DssCryptor.h"
#include "DssElementV2.h"

#include <tr1/memory>

using namespace std;

namespace {
		std::string strip_before_dot(const std::string& instr, size_t n=0) {
			size_t strof = instr.find_last_of(".");
			while (n > 0 && strof != instr.npos) {
				strof = instr.substr(0, strof-1).find_last_of(".");
				--n;
			}
			if (0 == strof) {
				return std::string("");
			}
			std::vector<char> leader(strof);
			memset(&leader[0], ' ', leader.size()-1); 
			leader.back() = 0;
			return	std::string(&leader[0]) + std::string("\"") + instr.substr(strof+1) + std::string("\"");
		}
}

#define OSTREAM_ASCII_T_EXTENDER(X) \
	ostream& operator<<(ostream& output, const DssFileFormat::X& p) {\
		output << p.get();\
		return output; \
	}

namespace std {

	OSTREAM_ASCII_T_EXTENDER(ascii3_t)
	OSTREAM_ASCII_T_EXTENDER(ascii4_t)
	OSTREAM_ASCII_T_EXTENDER(ascii6_t)
	OSTREAM_ASCII_T_EXTENDER(ascii8_t)
	OSTREAM_ASCII_T_EXTENDER(ascii16_t)
	OSTREAM_ASCII_T_EXTENDER(ascii20_t)
	OSTREAM_ASCII_T_EXTENDER(ascii100_t)
	OSTREAM_ASCII_T_EXTENDER(ascii100s_t)
	OSTREAM_ASCII_T_EXTENDER(ascii16s_t)
	OSTREAM_ASCII_T_EXTENDER(ascii16xff_t)

	ostream& operator<<(ostream& output, const DssFileFormat::byte_t& v) {
		output << (short)v;
		return output;
	}

}

namespace DssFileFormat {

	namespace v2 {

		LONG GetMaxIndexMarks(long releaseId) {
			return 99;
		}
		//EXTERN_GUID( ASF_EXTENDED_CONTENT_DESCRIPTION_OBJECT, 0xD2D0A440, 0xE307, 0x11D2, 0x97, 0xF0, 0x00, 0xA0, 0xC9, 0x5E, 0xA8, 0x50) ;

		struct CommonHeaderV2 : public ICommonHeader {

			CommonHeaderV2(const common_header_t& raw, bool saveOriginalData = true) {
				memcpy(&_data, &raw, sizeof(_data));
				if (_data.rec_start_date.get().size() == 0 || _data.rec_start_time.get().size() == 0) {
					set_date_time(_data.rec_start_date, _data.rec_start_time);
				}
				if (_data.rec_end_date.get().size() == 0 || _data.rec_end_time.get().size() == 0) {
					set_date_time(_data.rec_end_date, _data.rec_end_time);
				}
				
				if (saveOriginalData) {
					_originalHeader.reset( this->duplicate(false) );
				}
			}

			//CommonHeaderV2(const void *pMemory, size_t bytes, bool autoUpdateFromV1 = true) {
			//	if (0 == memcmp(pMemory, &ASF_EXTENDED_CONTENT_DESCRIPTION_OBJECT, sizeof(ASF_EXTENDED_CONTENT_DESCRIPTION_OBJECT))) {
			//		DssFileFormat::v2::wma_common_header_t wch;
			//		memcpy(&wch, pMemory, bytes);
			//		_data = wch.common_header;
			//	} 
			//	else {
			//		memcpy(&_data, pMemory, std::min<size_t>(bytes, sizeof(_data)));
			//	}
			//	common_header_t backupd = _data;
			//	if (_data.version_id != 2) {
			//		if (!autoUpdateFromV1) {
			//			throw std::runtime_error("DSS Version not 2, autoupdate set to false.");
			//		}
			//		std::auto_ptr<ICommonHeader> h122( DssFileFormat::v2::createHeaderFromV1(&_data, sizeof(_data), "wma" ) );
			//		this->_data = h122->fields();
			//	}
			//	if (_data.header_count != 3) {
			//		throw std::runtime_error("DSS Header count for V2 headers must be 3");
			//	}
			//	_originalHeader.reset( new CommonHeaderV2(backupd, false) );
			//}

			CommonHeaderV2(const char *fmt ="wav");

			ICommonHeader*				duplicate();
			ICommonHeader*				duplicate(bool keepOriginalData);
			virtual const ICommonHeader* originalHeader() const {
				return _originalHeader.get();
			}

			virtual common_header_t&	fields() {
				return _data;
			}
			virtual const common_header_t& fields() const {
				return _data;
			}
			virtual void				write(void *pDestination, size_t bytes, bool withFormatFraming) {
				if (bytes < sizeof(_data)) {
					throw std::runtime_error("Output buffer is too small to write DSS Common Header V2");
				}
				if (withFormatFraming) {
					if (this->selfIdentifier() == "mp3") {
						mp3_common_header_t toh;
						if (bytes < sizeof(toh)) {
							throw std::runtime_error("Output buffer is too small to write DSS Common Header V2 with MP3/ID3 framing.");
						}
						memcpy(&toh.id3.id, "ID3",3);
						toh.id3.flags = 0;
						toh.id3.version = 3;
						toh.id3.size = 0x5F6;
						memcpy(toh.frame.id, "XOLY", 4);
						toh.frame.flags = 0;
						toh.frame.size = 0x5EC;
						memcpy(&toh.frame.common_header, &_data, sizeof(_data));
						memcpy(pDestination, &toh, sizeof(toh));
					}
//					else if (this->selfIdentifier() == "wav") {
//						wav_common_header_t toh;
//						if (bytes < sizeof(toh)) {
//							throw std::runtime_error("Output buffer is too small to write DSS Common Header V2 with WAV framing.");
//						}
//						memcpy(toh.riff.ckID, "RIFF", 4);
//						toh.riff.ckSize = 0; //to be filled out
//						memcpy(toh.riff.WAVE, "WAVE", 4);
//						
//						memcpy(toh.fmt.ckID, "fmt ", 4);
//						toh.fmt.ckSize = 16;
//						{
//							WAVEFORMATEX *pwfx = (WAVEFORMATEX *)&toh.fmt.waveformat_ex; //beware not to set cbsize, since it doesn't exist here
//							pwfx->nChannels = 2;
//							pwfx->nSamplesPerSec = 44100;
//							pwfx->wFormatTag = WAVE_FORMAT_PCM;
//							pwfx->wBitsPerSample = 16;
//							pwfx->nBlockAlign = (pwfx->wBitsPerSample/8)*pwfx->nChannels;
//							pwfx->nAvgBytesPerSec = pwfx->nSamplesPerSec * pwfx->nBlockAlign;
//						}
//
//						memcpy(toh.olym.ckID,"olym",4);
//						toh.olym.ckSize = 0x5CC;
//						memcpy(&toh.olym.common_header, &_data, sizeof(_data));
//						
//						memcpy(toh.data.ckID, "data", 4);
//						toh.data.ckSize = 0x3CC;
//						memcpy(pDestination, &toh, sizeof(toh));
//					}
//					else if (this->selfIdentifier() == "wma") {
//#ifdef WIN32
//						const GUID ASF_EXTENDED_CONTENT_DESCRIPTION_OBJECT = {0xD2D0A440, 0xE307, 0x11D2, {0x97, 0xF0, 0x00, 0xA0, 0xC9, 0x5E, 0xA8, 0x50}};
//#else
//						EXTERN_GUID(ASF_EXTENDED_CONTENT_DESCRIPTION_OBJECT, 0xD2D0A440, 0xE307, 0x11D2, 0x97, 0xF0, 0x00, 0xA0, 0xC9, 0x5E, 0xA8, 0x50);
//#endif
//						wma_common_header_t toh;
//						if (bytes < sizeof(toh)) {
//							throw std::runtime_error("Output buffer is too small to write DSS Common Header V2 with WMA framing.");
//						}
//						memset(&toh, 0, sizeof(toh));
//						memcpy(&toh.extended_content_description.uuid, &ASF_EXTENDED_CONTENT_DESCRIPTION_OBJECT, sizeof(ASF_EXTENDED_CONTENT_DESCRIPTION_OBJECT));
//						toh.extended_content_description.object_size = 0x612;
//						toh.extended_content_description.content_descriptor_count = 1;
//						toh.extended_content_description.descriptor_name_length = 16;
//						strcpy(toh.extended_content_description.descriptor_name,"OLYMPUS");
//						toh.extended_content_description.descriptor_value_data_type = 1; //binary
//						toh.extended_content_description.descriptor_value_length = 0x5E2;
//						memcpy(&toh.common_header, &_data, sizeof(_data));
//						memcpy(pDestination, &toh, sizeof(toh));
//					}
					else {
						throw std::runtime_error("Unrecognized output format.");
					}
				} else {
					memcpy(pDestination, &_data, sizeof(_data));
				}
			}

			template<typename T>
			inline std::string get_fixed_string(const T& v) {
				std::vector<char> szTemp(sizeof(v)+1);
				memcpy(&szTemp[0], &v, sizeof(v));
				return std::string(&szTemp[0]);
			}
			template<typename T>
			inline void set_fixed_string(T& v, const std::string& newv) {
				memset(&v.value[0], ' ', sizeof(v));
				memcpy(&v.value[0], newv.c_str(), std::min<size_t>(sizeof(v), newv.length()));
			}
			virtual void				set(const std::string& key, const std::string& value);
			virtual std::string			get(const std::string& key);
			virtual std::string			get_binary(const std::string& key);
			virtual std::string			selfIdentifier(const char *newValue=0) ;
			virtual std::string			authorId(const char *newValue=0) ;
			virtual std::string			recStartDate(const char *newValue=0) ;
			virtual std::string			recStartTime(const char *newValue=0) ;
			virtual std::string			recEndDate(const char *newValue=0) ;
			virtual std::string			recEndTime(const char *newValue=0) ;
			virtual std::string			lengthTime(const char *newValue=0) ;
			virtual std::string			typistId(const char *newValue=0) ;
			virtual std::string			workTypeId(const char *newValue=0) ;
			virtual std::string			optionItemName1(const char *newValue=0) ;
			virtual std::string			optionItemId1(const char *newValue=0) ;
			virtual std::string			optionItemName2(const char *newValue=0) ;
			virtual std::string			optionItemId2(const char *newValue=0) ;
			virtual std::string			optionItemName3(const char *newValue=0) ;
			virtual std::string			optionItemId3(const char *newValue=0) ;
			virtual std::string			notes(const char *newValue=0) ;
			virtual std::vector<dword_t> indexMarks() ;
			virtual void				setIndexMarks(std::vector<dword_t> marks) ;
		protected:
			size_t						actualNumberOfIndexMarks() const;
#pragma pack(push, 16)
			common_header_t			_data;
		//	std::vector<shared_ptr<ICommonHeader> > _originalHeader;
			std::tr1::shared_ptr<ICommonHeader> _originalHeader;
#pragma pack(pop)
		};

		CommonHeaderV2::CommonHeaderV2(const char *fmt) {
			memset(&_data, 0, sizeof(_data) );
			byte_t *pdata = (byte_t*)&_data;
			//memcpy(pdata+12, "ODDSHOW", strlen("ODDSHOW"));
			memset(pdata+70, 0xFF, 16);
			memset(pdata+91, 0xFF, 15);
			memset(pdata+316, 0xFF, 1168);

			_data.header_count = 3;
			set_fixed_string(_data.self_identifier, fmt);
			_data.version_id = 2;
			_data.release_id = LATEST_RELEASE_ID;
			_data.licensee_id = 2;
			_data.object.voice_message = true;
			_data.job_number = 0xFFFFFFFF;
			_data.object.word = 0xFFFE;
			_data.process.word = 0xFFFE;
			_data.status.word = 0xFFF7;
			_data.attribute_flag = 0xFF;
			_data.priority_level = 0x07;
			_data.quality = (0 == strcmp("wma",fmt)) ? 0x17 : 0;
			_data.dss_status.flags = 0xFB;
			_data.typist_id.set("");
			_data.work_type_id.set("");
			_data.author_id.set("ODDSHOW");
			_data.option_item_id1.set("");
			_data.option_item_id2.set("");
			_data.option_item_id3.set("");
			_data.option_item_name1.set("");
			_data.option_item_name2.set("");
			_data.option_item_name3.set("");
			_data.notes.set("");

			_data.num_of_index_mark = GetMaxIndexMarks(LATEST_RELEASE_ID);
			memset(&_data.index_marks_1, 0xff, sizeof(_data.index_marks_1) );			
			memset(&_data.index_marks_2, 0xff, sizeof(_data.index_marks_2) );			
			memset(&_data.index_marks_3, 0xff, sizeof(_data.index_marks_3) );			

		}

		ICommonHeader* CommonHeaderV2::duplicate() {
				return new CommonHeaderV2(_data);
		}
		ICommonHeader* CommonHeaderV2::duplicate(bool withOriginalData) {
				return new CommonHeaderV2(_data, withOriginalData);
		}
		template<typename T>
		std::string set_and_get(T& field, const char *newValue) {
			if (newValue) { field.set(newValue); }
			return field.get();
		}
		std::string	CommonHeaderV2::selfIdentifier(const char *newValue) {
			return set_and_get(_data.self_identifier, newValue);
		}
		std::string	CommonHeaderV2::authorId(const char *newValue) {
			return set_and_get(_data.author_id, newValue);
		}
		std::string	CommonHeaderV2::recStartDate(const char *newValue) {
			return set_and_get(_data.rec_start_date, newValue);
		}
		std::string	CommonHeaderV2::recStartTime(const char *newValue) {
			return set_and_get(_data.rec_start_time, newValue);
		}
		std::string CommonHeaderV2::recEndDate(const char *newValue) {
			return set_and_get(_data.rec_end_date, newValue);
		}
		std::string	CommonHeaderV2::recEndTime(const char *newValue) {
			return set_and_get(_data.rec_end_time, newValue);
		}
		std::string	CommonHeaderV2::lengthTime(const char *newValue) {
			return set_and_get(_data.length, newValue);
		}
		std::string	CommonHeaderV2::typistId(const char *newValue) {
			return set_and_get(_data.typist_id, newValue);
		}
		std::string	CommonHeaderV2::workTypeId(const char *newValue) {
			return set_and_get(_data.work_type_id, newValue);
		}
		std::string	CommonHeaderV2::optionItemName1(const char *newValue) {
			return set_and_get(_data.option_item_name1, newValue);
		}
		std::string	CommonHeaderV2::optionItemId1(const char *newValue) {
			return set_and_get(_data.option_item_id1, newValue);
		}
		std::string	CommonHeaderV2::optionItemName2(const char *newValue) {
			return set_and_get(_data.option_item_name2, newValue);
		}
		std::string	CommonHeaderV2::optionItemId2(const char *newValue) {
			return set_and_get(_data.option_item_id2, newValue);
		}
		std::string	CommonHeaderV2::optionItemName3(const char *newValue) {
			return set_and_get(_data.option_item_name3, newValue);
		}
		std::string	CommonHeaderV2::optionItemId3(const char *newValue) {
			return set_and_get(_data.option_item_id3, newValue);
		}
		std::string	CommonHeaderV2::notes(const char *newValue) {
			return set_and_get(_data.notes, newValue);
		}
		size_t CommonHeaderV2::actualNumberOfIndexMarks() const {
			const dword_t * dwr = &_data.index_marks_1[0];
			size_t ret = 0;
			for (size_t k=0; k < GetMaxIndexMarks(); ++k) {
				if (dwr[k] != -1) {
					++ret;
				}
			}
			return ret;
		}
		std::vector<dword_t> CommonHeaderV2::indexMarks() {
			const dword_t * dwr = &_data.index_marks_1[0];
			std::vector<dword_t> ret;
			ret.reserve( actualNumberOfIndexMarks() );
			for (size_t k=0; k < GetMaxIndexMarks(); ++k) {
				if (dwr[k] != -1) {
					ret.push_back( dwr[k] );
				}
			}
			std::sort(ret.begin(), ret.end());
			ret.erase( std::unique(ret.begin(), ret.end()), ret.end());
			return ret;
		}
		void CommonHeaderV2::setIndexMarks(std::vector<dword_t> marks) {
			size_t max_size = (sizeof(_data.index_marks_1) + sizeof(_data.index_marks_2) + sizeof(_data.index_marks_3)) / sizeof(_data.index_marks_1[0]);
			if (marks.size() > max_size) {
				marks.resize(max_size);
			}
			std::sort(marks.begin(), marks.end());
			marks.erase( std::unique(marks.begin(), marks.end()), marks.end());
			memset(_data.index_marks_1, 0xff, sizeof(_data.index_marks_1));
			memset(_data.index_marks_2, 0xff, sizeof(_data.index_marks_2));
			memset(_data.index_marks_3, 0xff, sizeof(_data.index_marks_3));
			_data.num_of_index_mark = marks.size() > _data.num_of_index_mark ? v2::GetMaxIndexMarks() : _data.num_of_index_mark;
			if (marks.size() > 0) {
				memcpy(&_data.index_marks_1[0], &marks[0], std::min<size_t>(marks.size(),_data.num_of_index_mark) * sizeof(dword_t));
			}
		}
    
		std::string CommonHeaderV2::get(const std::string& key)
		{
			if (key == "typist_id") {
				return this->typistId();
			}
			else if (key == "author_id") {
				return this->workTypeId();
			}
			else if (key == "work_type_id") {
				return this->workTypeId();
			}
			else if (key == "option_item_name1") {
				return this->optionItemName1();
			}
			else if (key == "option_item_id1") {
				return this->optionItemId1();
			}
			else if (key == "option_item_name2") {
				return this->optionItemName2();
			}
			else if (key == "option_item_id2") {
				return this->optionItemName2();
			}
			else if (key == "option_item_name3") {
				return this->optionItemName3();
			}
			else if (key == "option_item_id3") {
				return this->optionItemName3();
			}
			else if (key == "notes") {
				return this->notes();
			}
			else if (key == "self_identifier") {
				return this->fields().self_identifier.get();
			}
			else if (key == "index_marks") {
				std::vector<dword_t> vs = this->indexMarks();
				std::stringstream ss;
				ss << "[" ;
				if (!vs.empty()) {
					ss << vs[0];
				}
				for (size_t k=1; k < vs.size(); ++k) {
					ss << ", " << vs[k];
				}
				ss << "]";
				return ss.str();
			}
//#define GET_PROCESS_KEY(x) \
//				else if (key == #x) {\
//					return boost::lexical_cast<std::string>(this->fields().x);\
//				}
            
//#warning itoa replaced with sprintf - bilal

#define GET_PROCESS_KEY(x) \
				else if (key == #x) {\
					std::stringstream ss;\
					char temp[64];\
					/*itoa(this->fields().x, temp, 10);*/\
                    sprintf(temp, "%d", this->fields().x);\
					ss << temp;\
					return ss.str();\
				}
				GET_PROCESS_KEY(version_id)
				GET_PROCESS_KEY(release_id)
				GET_PROCESS_KEY(licensee_id)
				GET_PROCESS_KEY(job_number)
				GET_PROCESS_KEY(object.word)
				GET_PROCESS_KEY(object.dictation)
				GET_PROCESS_KEY(object.voice_message)
				GET_PROCESS_KEY(process.word)
				GET_PROCESS_KEY(process.recording)
				GET_PROCESS_KEY(process.playing)
				GET_PROCESS_KEY(process.typing)
				GET_PROCESS_KEY(process.autotranscribing)
				GET_PROCESS_KEY(process.archiving)
				GET_PROCESS_KEY(status.word)
				GET_PROCESS_KEY(status.recording)
				GET_PROCESS_KEY(status.playing)
				GET_PROCESS_KEY(status.typing)
				GET_PROCESS_KEY(status.autotranscribing)
				GET_PROCESS_KEY(attribute_flag)
				GET_PROCESS_KEY(priority_level)
				GET_PROCESS_KEY(ex_rec_length)
				GET_PROCESS_KEY(quality)
				GET_PROCESS_KEY(dss_status.flags)
				GET_PROCESS_KEY(dss_status.author_download)
				GET_PROCESS_KEY(dss_status.typist_download)
				GET_PROCESS_KEY(dss_status.remote_dictation)
				GET_PROCESS_KEY(dss_status.author_import)
				GET_PROCESS_KEY(dss_status.typist_import)
				GET_PROCESS_KEY(dss_status.send)
				GET_PROCESS_KEY(priority_status)
				GET_PROCESS_KEY(playback_position)
				GET_PROCESS_KEY(num_of_index_mark)
				
				throw std::runtime_error("key not found");
		}
		template<typename T>
		std::string as_binary_string(const T& v) {
			const char *beg = static_cast<const char *>(static_cast<const void *>(&v));
			return std::string(beg, beg + sizeof(T));
		}
		std::string CommonHeaderV2::get_binary(const std::string& key)
		{
			if (key == "typist_id") {
				return as_binary_string(_data.typist_id);
			}
			else if (key == "author_id") {
				return as_binary_string(_data.author_id);
			}
			else if (key == "work_type_id") {
				return as_binary_string(_data.work_type_id);
			}
			else if (key == "option_item_name1") {
				return as_binary_string(_data.option_item_name1);
			}
			else if (key == "option_item_id1") {
				return as_binary_string(_data.option_item_id1);
			}
			else if (key == "option_item_name2") {
				return as_binary_string(_data.option_item_name2);
			}
			else if (key == "option_item_id2") {
				return as_binary_string(_data.option_item_id2);
			}
			else if (key == "option_item_name3") {
				return as_binary_string(_data.option_item_name3);
			}
			else if (key == "option_item_id3") {
				return as_binary_string(_data.option_item_id3);
			}
			else if (key == "notes") {
				return as_binary_string(_data.notes);
			}
			else if (key == "self_identifier") {
				return as_binary_string(_data.self_identifier);
			}
			else if (key == "index_marks") {
				return as_binary_string(_data.index_marks_1) + as_binary_string(_data.index_marks_2) + as_binary_string(_data.index_marks_3);
			}
#define GET_PROCESS_BKEY(x) \
				else if (key == #x) {\
					return as_binary_string(_data.x);\
				}
				GET_PROCESS_BKEY(version_id)
				GET_PROCESS_BKEY(release_id)
				GET_PROCESS_BKEY(licensee_id)
				GET_PROCESS_BKEY(job_number)
				GET_PROCESS_BKEY(object.word)
				GET_PROCESS_BKEY(process.word)
				GET_PROCESS_BKEY(status.word)
				GET_PROCESS_BKEY(attribute_flag)
				GET_PROCESS_BKEY(priority_level)
				GET_PROCESS_BKEY(ex_rec_length)
				GET_PROCESS_BKEY(quality)
				GET_PROCESS_BKEY(dss_status.flags)
				GET_PROCESS_BKEY(priority_status)
				GET_PROCESS_BKEY(playback_position)
				GET_PROCESS_BKEY(num_of_index_mark)

				throw std::runtime_error("key not found");
		}		
		void CommonHeaderV2::set(const std::string& key, const std::string& rvalue)
		{
			try {
				std::string value = rvalue;
				if (value == "NOW") {
					ascii6_t	tempt[2];
					v2::set_date_time(tempt[0], tempt[1]);
					if (key.find("time")) {
						value = tempt[1].get();
					} else {
						value = tempt[0].get();
					}
					if (key == "rec_start") {
						this->recStartDate(tempt[0].get().c_str());
						this->recStartTime(tempt[1].get().c_str());
						return;
					}
					else if (key == "rec_end") {
						this->recEndDate(tempt[0].get().c_str());
						this->recEndTime(tempt[1].get().c_str());
						return;
					}
				}
				if (key == "rec_start_time") {
					this->recStartTime(value.c_str());
				}
				else if (key == "rec_start_date") {
					this->recStartDate(value.c_str());
				}
				else if (key == "rec_end_time") {
					this->recStartTime(value.c_str());
				}
				else if (key == "rec_end_date") {
					this->recStartDate(value.c_str());
				}
				else if (key == "author_id") {
					this->authorId(value.c_str());
				}
				else if (key == "length") {
					this->lengthTime(value.c_str());
				}
				else if (key == "typist_id") {
					this->typistId(value.c_str());
				}
				else if (key == "work_type_id") {
					this->workTypeId(value.c_str());
				}
				else if (key == "option_item_name1") {
					this->optionItemName1(value.c_str());
				}
				else if (key == "option_item_id1") {
					this->optionItemId1(value.c_str());
				}
				else if (key == "option_item_name2") {
					this->optionItemName2(value.c_str());
				}
				else if (key == "option_item_id2") {
					this->optionItemName2(value.c_str());
				}
				else if (key == "option_item_name3") {
					this->optionItemName3(value.c_str());
				}
				else if (key == "option_item_id3") {
					this->optionItemName3(value.c_str());
				}
				else if (key == "notes") {
					this->notes(value.c_str());
				}
				else if (key == "self_identifier") {
					this->fields().self_identifier.set(value);
				}
//#define PROCESS_KEY(x) \
//				else if (key == #x) {\
//					this->fields().x = boost::lexical_cast<DWORD>(value);\
//				}
//#define PROCESS_KEYB(x) \
//				else if (key == #x) {\
//					if (value == "true") {\
//						this->fields().x = true;\
//					} else if (value == "false") {\
//						this->fields().x = false;\
//					} else {\
//						this->fields().x = boost::lexical_cast<int>(value);\
//					}\
//				}
#define PROCESS_KEY(x) \
				else if (key == #x) {\
					this->fields().x = (DWORD)atoi(value.c_str());\
				}
#define PROCESS_KEYB(x) \
				else if (key == #x) {\
					if (value == "true") {\
						this->fields().x = true;\
					} else if (value == "false") {\
						this->fields().x = false;\
					} else {\
						this->fields().x = (int)atoi(value.c_str());\
					}\
				}
				PROCESS_KEY(version_id)
				PROCESS_KEY(release_id)
				PROCESS_KEY(licensee_id)
				PROCESS_KEY(job_number)
				PROCESS_KEY(object.word)
				PROCESS_KEYB(object.dictation)
				PROCESS_KEYB(object.voice_message)
				PROCESS_KEY(process.word)
				PROCESS_KEYB(process.recording)
				PROCESS_KEYB(process.playing)
				PROCESS_KEYB(process.typing)
				PROCESS_KEYB(process.autotranscribing)
				PROCESS_KEYB(process.archiving)
				PROCESS_KEY(status.word)
				PROCESS_KEYB(status.recording)
				PROCESS_KEYB(status.playing)
				PROCESS_KEYB(status.typing)
				PROCESS_KEYB(status.autotranscribing)
				PROCESS_KEY(attribute_flag)
				PROCESS_KEY(priority_level)
				PROCESS_KEY(ex_rec_length)
				PROCESS_KEY(quality)
				PROCESS_KEY(dss_status.flags)
				PROCESS_KEYB(dss_status.author_download)
				PROCESS_KEYB(dss_status.typist_download)
				PROCESS_KEYB(dss_status.remote_dictation)
				PROCESS_KEYB(dss_status.author_import)
				PROCESS_KEYB(dss_status.typist_import)
				PROCESS_KEYB(dss_status.send)
				PROCESS_KEY(priority_status)
				PROCESS_KEY(playback_position)
				else if (key == "index_marks") {
					std::vector<dword_t> marks;
					size_t off = 0;
					while (off != value.npos && off < value.length()) {
						size_t noff = value.find(",", off);
						std::string v = value.substr(off,noff-off);
						marks.push_back( (dword_t)atoi(v.c_str()) );
						off += v.length()+1;
					}
					this->setIndexMarks(marks);
				}
			}
			//catch(boost::bad_lexical_cast& e) {
			//	std::cerr << "Error in header set (bad lexical cast): " << e.what() << std::endl;
			//}
			catch(std::exception& e) {
				std::cerr << "Error in header set: " << e.what() << std::endl;
			}
			catch(...) {
				std::cerr << "Unknown error ocurred in header set." << std::endl;
			}
		}
		ICommonHeader* createHeader(const char *fmt) {
			return new CommonHeaderV2(fmt);
		}

		//ICommonHeader* createHeaderFromMemory(const void *pMemory, size_t bytes, bool autoUpdateFromV1) {
		//	return new CommonHeaderV2(pMemory, bytes, autoUpdateFromV1);
		//}

		void writeToV1(ICommonHeader* pch, DssFileFormat::DssCommonHeader& ch, DssFileFormat::DssHeader *secondHeader, DssFileFormat::DssHeader *thirdHeader) {
			size_t hbn = 1;
			if (secondHeader ) {
				++hbn;
				if (thirdHeader) {
					++hbn;
				}
			}
			ch.set_HeaderBlockNum( hbn );
			ch.set_AuthorId( pch->authorId() );
			ch.set_SelfIdentifier( "dss" );
			ch.set_VersionId(1);
			ch.set_ReleaseId(2);
			ch.set_JobNumber( pch->fields().job_number );
			ch.set_AttributeFlag( pch->fields().attribute_flag );
			ch.set_ObjectWord( pch->fields().object.word );
			ch.set_ProcessWord( pch->fields().process.word );
			ch.set_PriorityLevel( pch->fields().priority_level );
			ch.set_StatusWord( pch->fields().status.word );

			ch.set_TypistId( pch->fields().typist_id.get() );

			ch.set_RecordingStartDate( pch->fields().rec_start_date.get() );
			ch.set_RecordingStartTime( pch->fields().rec_start_time.get() );
			ch.set_RecordingEndDate( pch->fields().rec_end_date.get() );
			ch.set_RecordingEndTime( pch->fields().rec_end_time.get() );
			ch.set_Length( pch->fields().length.get() );

			std::vector<dword_t> indexMarks = pch->indexMarks();

			ch.UpdateRaw();

			if (secondHeader) {
				if (secondHeader->GetHeaderType() == DssFileFormat::DSS_OPTIONAL_HEADER) {
					DssFileFormat::DssOptionalHeader *doh = static_cast<DssOptionalHeader *>(secondHeader);
					doh->set_DssStatus( pch->fields().dss_status.flags );
					doh->set_ExRecLength( pch->fields().ex_rec_length );
					doh->set_OptionItemId1(  pch->fields().option_item_id1.get() );
					doh->set_OptionItemId2( pch->fields().option_item_id2.get() );
					doh->set_OptionItemId3( pch->fields().option_item_id3.get() );
					doh->set_OptionItemName1( pch->fields().option_item_name1.get() );
					doh->set_OptionItemName2( pch->fields().option_item_name2.get() );
					doh->set_OptionItemName3( pch->fields().option_item_name3.get() );
					doh->set_PlaybackPosition( pch->fields().playback_position );
					doh->set_PriorityStatus( pch->fields().priority_status );
					doh->set_Quality( pch->fields().quality );
					doh->set_WorkTypeId( pch->fields().work_type_id.get() );
					doh->set_Notes( pch->fields().notes.get() );
					
					DssFileFormat::extended_index_mark_t mark;
					for (size_t k=0; k < 16; ++k) {
						if (k < indexMarks.size()) {
							memset(&mark, 0, sizeof(mark) );
							mark.dw_position = indexMarks[k];
						} else {
							memset(&mark, 0xff, sizeof(mark) );
						}
						doh->set_ExtendedIndexMark((ULONG)k, mark);
					}
				}
				else if (secondHeader->GetHeaderType() == DssFileFormat::DSS_PRO_OPTIONAL_HEADER) {
					DssFileFormat::DssProOptionalHeader *doh = static_cast<DssProOptionalHeader *>(secondHeader);
					doh->set_ExRecLength( pch->fields().ex_rec_length );
					doh->set_Notes( pch->fields().notes.get() );
					doh->set_PlaybackPosition( pch->fields().playback_position );
					doh->set_PriorityStatus( pch->fields().priority_status );
					doh->set_Quality( pch->fields().quality );
					doh->set_WorkTypeId( pch->fields().work_type_id.get() );
					
				}
				secondHeader->UpdateRaw();
			}
			return;
		}

		void writeToV1(ICommonHeader* pch, DssFileFormat::DssScanner& scanner)
		{
			writeToV1(pch, *scanner.GetCommonHeader(), scanner.GetIndexedHeader(1), scanner.GetCommonHeader()->get_HeaderBlockNum() > 2 ? scanner.GetIndexedHeader(2) : 0);

			// fill in block/offset data if possible
			std::vector<dword_t> indexMarks = pch->indexMarks();
			if (scanner.GetCommonHeader()->get_VersionId() == 1 && scanner.GetCommonHeader()->get_HeaderBlockNum() > 1) {
				// standard block/frame/milli imarks
				for (size_t k=0; k < std::min<size_t>(indexMarks.size(), 16); ++k) {
					DssFileFormat::extended_index_mark_t exim;
					exim.dw_position = indexMarks[k];
					LONGLONG llTime = (LONGLONG)exim.dw_position * UNITS / (LONGLONG)DssFileFormat::get_samplerate_for_mode(scanner.GetPayloadCompressionMode());
					exim.dw_sector_offset = scanner.GetBlockOffsetForTime( llTime );
					exim.w_frame_offset = 0; //block accuracy, at least for now;
					static_cast<DssFileFormat::DssOptionalHeader *>(scanner.GetIndexedHeader(1))->set_ExtendedIndexMark((ULONG)k, exim);
				}
			}
			else if (scanner.GetCommonHeader()->get_VersionId() == 2 && scanner.GetCommonHeader()->get_HeaderBlockNum() > 1) {
				// standard block/frame/milli imarks
				DssFileFormat::file_header_ds2_t fhd2;
				memset(&fhd2, 0xff, sizeof(fhd2));
				for (size_t k=0; k < std::min<size_t>(indexMarks.size(), 16); ++k) {
					LONGLONG llTime = (LONGLONG)indexMarks[k] * UNITS / (LONGLONG)DssFileFormat::get_samplerate_for_mode(scanner.GetPayloadCompressionMode());
					fhd2.blocksector_marks[k].block_offset = scanner.GetBlockOffsetForTime( llTime );
					fhd2.blocksector_marks[k].frame_offset= 0; //block accuracy, at least for now;
				}
				static_cast<DssFileFormat::DssCommonHeader *>(scanner.GetIndexedHeader(1))->set_AdditionalIMark(&fhd2.additional_instruction_mark[0]);
			}
		}

		ICommonHeader* createHeaderFromV1(const void *pVersion1Raw, size_t bytes, const char *containerFormat) {
			DssFileFormat::v2::common_header_t cht;
			memset(&cht, 0xff, sizeof(cht) );
			memcpy(&cht, pVersion1Raw, std::min<size_t>(sizeof(cht), bytes));
			std::auto_ptr<ICommonHeader> ret( new CommonHeaderV2(cht) );

			std::auto_ptr<ICommonHeader> cleanh( DssFileFormat::v2::createHeader(containerFormat) );
			memcpy(&(ret->fields()), &(cleanh->fields()), sizeof( ret->fields() ) );

			ret->fields().num_of_index_mark = GetMaxIndexMarks(LATEST_RELEASE_ID);
			ret->fields().self_identifier.set( containerFormat );
			ret->fields().header_count = 3;
			ret->fields().version_id = 2;
			ret->fields().release_id = LATEST_RELEASE_ID;
			ret->fields().licensee_id = 2;

			DssFileFormat::DssScanner scanner;
			scanner.Fill((const byte_t *)pVersion1Raw, (long)bytes);
			DssElement* pel;
			DssElement* dch;

			unsigned long processed_flag = 0;
			bool had_blocks = false;
			long bcount = 0;
			while (!had_blocks) {
				pel = scanner.NextItem();
				++bcount;
				if (!pel) {
					break;
				}
				DSS_HEADER_TYPE headerType = pel->GetElementType() == DSS_FILE_HEADER ? static_cast<DssHeader *>(pel)->GetHeaderType() : DSS_UNKNOWN_HEADER;
				bool is_repeat = false;
				if (processed_flag & (1 << (long)headerType)) {
					is_repeat = true;
				}
				processed_flag = processed_flag | (1 << (long)headerType);
				
				switch(headerType)
				{
					case DssFileFormat::DSS_COMMON_HEADER:
						if (is_repeat) {
							break;
						}
						ret->fields().attribute_flag	= static_cast<DssCommonHeader *>(pel)->get_AttributeFlag();
						ret->fields().author_id			= static_cast<DssCommonHeader *>(pel)->get_AuthorId();
						ret->fields().ex_rec_length		= 0;// convert -> static_cast<DssCommonHeader *>(pel)->get_LengthOfRecording();
						ret->fields().job_number		= static_cast<DssCommonHeader *>(pel)->get_JobNumber();
						ret->fields().length			= static_cast<DssCommonHeader *>(pel)->get_LengthOfRecording();
						ret->fields().licensee_id		= static_cast<DssCommonHeader *>(pel)->get_LicenseeId();
						ret->fields().object.word		= static_cast<DssCommonHeader *>(pel)->get_ObjectWord();
						ret->fields().priority_level	= static_cast<DssCommonHeader *>(pel)->get_PriorityLevel();
						ret->fields().status.word		= static_cast<DssCommonHeader *>(pel)->get_StatusWord();
						ret->fields().process.word		= static_cast<DssCommonHeader *>(pel)->get_ProcessWord();
						
						ret->fields().rec_start_date	= static_cast<DssCommonHeader *>(pel)->get_RecordingStartDate();
						ret->fields().rec_start_time	= static_cast<DssCommonHeader *>(pel)->get_RecordingStartTime();
						ret->fields().rec_end_date		= static_cast<DssCommonHeader *>(pel)->get_RecordingEndDate();
						ret->fields().rec_end_time		= static_cast<DssCommonHeader *>(pel)->get_RecordingEndTime();
						ret->fields().typist_id			= static_cast<DssCommonHeader *>(pel)->get_TypistId();
						dch = pel;
						break;
					case DssFileFormat::DSS_OPTIONAL_HEADER:
						if (is_repeat) {
							break;
						}
						{
							ret->fields().quality			= static_cast<DssOptionalHeader *>(pel)->get_Quality();
							ret->fields().notes				= static_cast<DssOptionalHeader *>(pel)->get_Notes();
							ret->fields().dss_status.flags	= static_cast<DssOptionalHeader *>(pel)->get_DssStatus();
							ret->fields().ex_rec_length		= static_cast<DssOptionalHeader *>(pel)->get_ExRecLength();
							ret->fields().option_item_id1	= static_cast<DssOptionalHeader *>(pel)->get_OptionItemId1();
							ret->fields().option_item_id2	= static_cast<DssOptionalHeader *>(pel)->get_OptionItemId2();
							ret->fields().option_item_id3	= static_cast<DssOptionalHeader *>(pel)->get_OptionItemId3();
							ret->fields().option_item_name1	= static_cast<DssOptionalHeader *>(pel)->get_OptionItemName1();
							ret->fields().option_item_name2	= static_cast<DssOptionalHeader *>(pel)->get_OptionItemName2();
							ret->fields().option_item_name3	= static_cast<DssOptionalHeader *>(pel)->get_OptionItemName3();
							ret->fields().playback_position = static_cast<DssOptionalHeader *>(pel)->get_PlaybackPosition();
							ret->fields().priority_status	= static_cast<DssOptionalHeader *>(pel)->get_PriorityStatus();
							ret->fields().work_type_id		= static_cast<DssOptionalHeader *>(pel)->get_WorkTypeId();
							
							DssFileFormat::extended_index_mark_t exm;
							std::vector<dword_t> marks;
							for(size_t k=0; k < 16; ++k) {
								exm = static_cast<DssOptionalHeader *>(pel)->get_ExtendedIndexMark((int)k);
								if (exm.dw_position != -1) {
									marks.push_back(exm.dw_position);
								}
								else {
									break;
								}
							}
							ret->setIndexMarks(marks);
						}
						break;
					case DssFileFormat::DSS_PRO_OPTIONAL_HEADER:
						if (is_repeat) {
							break;
						}
						ret->fields().quality			= static_cast<DssProOptionalHeader *>(pel)->get_Quality();
						ret->fields().notes				= static_cast<DssProOptionalHeader *>(pel)->get_Notes();

						ret->fields().ex_rec_length		= static_cast<DssProOptionalHeader *>(pel)->get_ExRecLength();
						ret->fields().option_item_id1	= static_cast<DssProOptionalHeader *>(pel)->get_OptionItemId(1);
						ret->fields().option_item_id2	= static_cast<DssProOptionalHeader *>(pel)->get_OptionItemId(2);
						ret->fields().option_item_id3	= static_cast<DssProOptionalHeader *>(pel)->get_OptionItemId(3);
						ret->fields().option_item_name1	= static_cast<DssProOptionalHeader *>(pel)->get_OptionItemName(1);
						ret->fields().option_item_name2	= static_cast<DssProOptionalHeader *>(pel)->get_OptionItemName(2);
						ret->fields().option_item_name3	= static_cast<DssProOptionalHeader *>(pel)->get_OptionItemName(3);
						ret->fields().playback_position = static_cast<DssProOptionalHeader *>(pel)->get_PlaybackPosition();
						ret->fields().priority_status	= static_cast<DssProOptionalHeader *>(pel)->get_PriorityStatus();
						ret->fields().work_type_id		= static_cast<DssProOptionalHeader *>(pel)->get_WorkTypeId();
						break;
					case DssFileFormat::DSS_PRO_OPTIONAL_HEADER2:
						break;
					default:
						had_blocks = true;
						break;
				}
				if (had_blocks && ret->indexMarks().empty()) {
					std::vector<dword_t> marks;
					scanner.FlushBlocks(false);
					// get them from the common header
					LONGLONG modeFrameLength = UNITS * (LONGLONG)get_framelength_for_mode(scanner.GetCompressionMode()) / 
															(LONGLONG)get_samplerate_for_mode(scanner.GetCompressionMode());
					for (size_t k=0; k < 16; ++k) {
						const file_header_t *fhp = static_cast<const DssFileFormat::file_header_t *>(static_cast<const void *>(static_cast<DssCommonHeader *>(dch)->Data()));
						if (0xFFFF == fhp->blocksector_marks[k].block_offset && 
							0xFF == fhp->blocksector_marks[k].frame_offset) {
								break;
						}
						LONGLONG llBase = scanner.GetTimeForFileOffset(512L * fhp->blocksector_marks[k].block_offset );
						llBase += modeFrameLength * fhp->blocksector_marks[k].frame_offset;
						marks.push_back( (dword_t)(llBase / 100000L ) );
					}
					ret->setIndexMarks(marks);
				}
			}
			scanner.Empty();
			return ret.release();
		}

		void inheritFromHeader(ICommonHeader *dest, const ICommonHeader *src, bool isDirty)
		{
			if (!src) {
				return;
			}
			dest->fields().author_id.set( src->fields().author_id.get() );
			dest->fields().dss_status.flags = src->fields().dss_status.flags;
			dest->fields().job_number = src->fields().job_number;
			dest->fields().object.word = src->fields().object.word;
			dest->fields().process.word = src->fields().process.word;
			dest->fields().status.word = src->fields().status.word;
			dest->fields().attribute_flag = src->fields().attribute_flag;
			dest->fields().priority_level = src->fields().priority_level;
			dest->fields().typist_id.set( src->fields().typist_id.get() );

			dest->fields().rec_start_date.set( src->fields().rec_start_date.get() );
			dest->fields().rec_start_time.set( src->fields().rec_start_time.get() );
			if (!isDirty) {
				dest->fields().rec_end_date.set( src->fields().rec_end_date.get() );
				dest->fields().rec_end_time.set( src->fields().rec_end_time.get() );
			}
			dest->fields().priority_status = src->fields().priority_status;
			dest->notes(src->fields().notes.get().c_str());
		}

		void set_date_time(ascii6_t& theDate, ascii6_t& theTime, std::tm *timeStamp)
		{
			if (!timeStamp) {
				std::time_t ntst = std::time(NULL);
				timeStamp =  std::localtime(&ntst);
			}
			// Ã†â€™XÃ†â€™}Ã†â€™zÃ†â€™AÃ†â€™vÃ†â€™Ã…Â Ã¢â‚¬Å¡Ãƒâ€°Ã…Â Ãƒâ€“Ã¢â‚¬Å¡Ã‚ÂµÃ¢â‚¬Å¡Ãƒâ€žÃ¢â‚¬Å¡Ãƒï¿½Ã¯Â¿Â½AÃ¢â‚¬Â¢KÃ¢â‚¬Å¡Ã‚Â¸DssFormatterÃ…â€™oÃ¢â‚¬â€�RÃ¢â‚¬Å¡Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂºÃ…Â½Ã…Â¾Ã¢â‚¬Å¡Ã‚ÂªÃ¯Â¿Â½Ãƒï¿½Ã¢â‚¬â„¢ÃƒÂ¨Ã¢â‚¬Å¡Ã‚Â³Ã¢â‚¬Å¡ÃƒÂªÃ¢â‚¬Å¡ÃƒÂ©
			//std::string datestr = boost::str(boost::format("%02d%02d%02d") % (timeStamp->tm_year%100) % (timeStamp->tm_mon+1) % timeStamp->tm_mday);
			//std::string timestr = boost::str(boost::format("%02d%02d%02d") % timeStamp->tm_hour % timeStamp->tm_min % timeStamp->tm_sec);
			std::string datestr = "000000";
			std::string timestr = "000000";
			theDate.set(datestr);
			theTime.set(timestr);
		}
		
		void set_length(ascii6_t& theLength, dword_t length_inMsec)
		{
			std::time_t lent = (std::time_t)ceil( length_inMsec * 0.001 );
			std::tm dt = *std::gmtime(&lent);
			//std::string timestr = boost::str(boost::format("%02d%02d%02d") % dt.tm_hour % dt.tm_min % dt.tm_sec);
			std::string timestr = "000000";
			theLength.set(timestr);
		}

#define FIELD_TO_STREAM(strm, x) strm << strip_before_dot(#x) << " : " <<  x << "," << std::endl
#define FIELD_TO_STREAM_S(strm, x) strm << strip_before_dot(#x) << " : " <<  (x.value[0] == -1 ? "null" : (std::string("\"") + std::string(x.get().c_str()) + "\"")) << ", " <<  std::endl
#define FIELD_TO_STREAM_B(strm, x) strm << strip_before_dot(#x) << " : " <<  (!x) << " /* (" << ((int)x) << ") */," << std::endl
#define FIELD_TO_STREAM_GROUP_BEGIN(strm, x) strm << strip_before_dot(#x,1).replace(strip_before_dot(#x,1).find("."),1,"_") << ": {" << std::endl;
#define FIELD_TO_STREAM_END_B(strm, x) strm << strip_before_dot(#x) << " : " <<  (!x) << " /* (" << ((int)x) << ") */ }, " << std::endl
#define FIELD_TO_STREAM_GROUP(strm, x) FIELD_TO_STREAM_GROUP_BEGIN(strm, x); strm << strip_before_dot(#x) << " : " << x <<	" /* 0x" << std::hex << x << std::dec << "*/ ," << std::endl
#define ARRAY_FIELD_TO_STREAM(strm, x)\
		{ \
		strm << strip_before_dot(#x) << " : [";\
		for (size_t k=0; k < sizeof(x)/sizeof(x[0]); ++k) { \
		if(k>0) { strm << ", "; } \
		strm << x[k]; }\
		strm << "]";\
		}

		std::string to_text(const ICommonHeader* header) {
			std::stringstream ss;

			ss << std::boolalpha;

			FIELD_TO_STREAM(ss, header->fields().header_count);
			FIELD_TO_STREAM_S(ss, header->fields().self_identifier);
			FIELD_TO_STREAM(ss, header->fields().version_id);
			FIELD_TO_STREAM(ss, header->fields().release_id);
			FIELD_TO_STREAM(ss, header->fields().licensee_id);
			FIELD_TO_STREAM_S(ss, header->fields().author_id);
			FIELD_TO_STREAM(ss, header->fields().job_number);
			FIELD_TO_STREAM_GROUP(ss, header->fields().object.word);
			FIELD_TO_STREAM_B(ss, header->fields().object.dictation);
			FIELD_TO_STREAM_END_B(ss, header->fields().object.voice_message);
			FIELD_TO_STREAM_GROUP(ss, header->fields().process.word);
			FIELD_TO_STREAM_B(ss, header->fields().process.recording);
			FIELD_TO_STREAM_B(ss, header->fields().process.playing);
			FIELD_TO_STREAM_B(ss, header->fields().process.typing);
			FIELD_TO_STREAM_B(ss, header->fields().process.autotranscribing);
			FIELD_TO_STREAM_END_B(ss, header->fields().process.archiving);
			FIELD_TO_STREAM_GROUP(ss, header->fields().status.word);
			FIELD_TO_STREAM_B(ss, header->fields().status.recording);
			FIELD_TO_STREAM_B(ss, header->fields().status.playing);
			FIELD_TO_STREAM_B(ss, header->fields().status.typing);
			FIELD_TO_STREAM_END_B(ss, header->fields().status.autotranscribing);
			FIELD_TO_STREAM_S(ss, header->fields().rec_start_date);
			FIELD_TO_STREAM_S(ss, header->fields().rec_start_time);
			FIELD_TO_STREAM_S(ss, header->fields().rec_end_date);
			FIELD_TO_STREAM_S(ss, header->fields().rec_end_time);
			FIELD_TO_STREAM_S(ss, header->fields().length);
			FIELD_TO_STREAM(ss, header->fields().attribute_flag);
			FIELD_TO_STREAM(ss, header->fields().priority_level);
			FIELD_TO_STREAM_S(ss, header->fields().typist_id);
			FIELD_TO_STREAM(ss, header->fields().ex_rec_length);
			FIELD_TO_STREAM(ss, header->fields().quality);
			FIELD_TO_STREAM_S(ss, header->fields().work_type_id);
			FIELD_TO_STREAM_S(ss, header->fields().option_item_name1);
			FIELD_TO_STREAM_S(ss, header->fields().option_item_id1);
			FIELD_TO_STREAM_S(ss, header->fields().option_item_name2);
			FIELD_TO_STREAM_S(ss, header->fields().option_item_id2);
			FIELD_TO_STREAM_S(ss, header->fields().option_item_name3);
			FIELD_TO_STREAM_S(ss, header->fields().option_item_id3);
			FIELD_TO_STREAM_GROUP(ss, header->fields().dss_status.flags);
			FIELD_TO_STREAM_B(ss, header->fields().dss_status.author_download);
			FIELD_TO_STREAM_B(ss, header->fields().dss_status.typist_download);
			FIELD_TO_STREAM_B(ss, header->fields().dss_status.remote_dictation);
			FIELD_TO_STREAM_B(ss, header->fields().dss_status.author_import);
			FIELD_TO_STREAM_B(ss, header->fields().dss_status.typist_import);
			FIELD_TO_STREAM_END_B(ss, header->fields().dss_status.send);
			FIELD_TO_STREAM(ss, header->fields().priority_status);
			FIELD_TO_STREAM(ss, header->fields().playback_position);
			FIELD_TO_STREAM_S(ss, header->fields().notes);
			FIELD_TO_STREAM(ss, header->fields().num_of_index_mark);
			ARRAY_FIELD_TO_STREAM(ss, header->fields().index_marks_1); 
			ss << "," << std::endl;
			ARRAY_FIELD_TO_STREAM(ss, header->fields().index_marks_2); 
			ss << "," << std::endl;
			ARRAY_FIELD_TO_STREAM(ss, header->fields().index_marks_3); 
			ss << std::endl;
			return ss.str();
		}
	}
};
